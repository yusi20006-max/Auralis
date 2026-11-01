package com.auralis.app.data

import android.content.Context
import android.net.Uri
import com.auralis.app.data.catalog.CatalogConfig
import com.auralis.app.data.catalog.CatalogEpisode
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.db.AuralisDatabase
import com.auralis.app.data.db.EpisodeDao
import com.auralis.app.data.db.EpisodeEntity
import com.auralis.app.data.download.DownloadCancelledException
import com.auralis.app.data.download.DownloadIdentity
import com.auralis.app.data.download.DownloadStore
import com.auralis.app.data.download.InsufficientSpaceException
import com.auralis.app.data.download.PrivateDownloadStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LibraryRepository internal constructor(
    private val dao: EpisodeDao,
    private val downloadStore: DownloadStore,
    private val scope: CoroutineScope,
) {
    constructor(context: Context) : this(
        AuralisDatabase.get(context).episodeDao(),
        PrivateDownloadStore(context),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    val episodes: StateFlow<List<Episode>> = dao.observeAll()
        .map { list -> list.map { it.toEpisode() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentEpisodeId = MutableStateFlow<String?>(null)
    val currentEpisodeId: StateFlow<String?> = _currentEpisodeId.asStateFlow()

    private val _downloadingIdentities = MutableStateFlow<Set<DownloadIdentity>>(emptySet())
    val downloadingIdentities: StateFlow<Set<DownloadIdentity>> = _downloadingIdentities.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<DownloadIdentity, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<DownloadIdentity, Float>> = _downloadProgress.asStateFlow()

    private val _downloadErrors = MutableStateFlow<Map<DownloadIdentity, String>>(emptyMap())
    val downloadErrors: StateFlow<Map<DownloadIdentity, String>> = _downloadErrors.asStateFlow()

    /** Compatibility summary for the existing app-wide error banner. */
    val lastDownloadError: StateFlow<String?> = _downloadErrors
        .map { errors -> errors.values.lastOrNull() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    // A single identity serializes its own requests; different sources never share a lock.
    private val downloadMutexes = ConcurrentHashMap<DownloadIdentity, Mutex>()

    suspend fun addLocalEpisode(
        uri: Uri,
        title: String,
        durationMs: Long = 0L,
        artworkUri: Uri? = null,
    ): Episode {
        val candidate = Episode(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "اپیزود بدون عنوان" },
            podcastName = "Auralis",
            durationMs = durationMs,
            artworkUri = artworkUri,
            localUri = uri,
            isAiGenerated = true,
            sourceId = null,
        )
        return dao.getOrInsertLocalEpisode(EpisodeEntity.from(candidate)).toEpisode()
    }

    fun addOrGetStreamEpisode(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode {
        val sourceId = CatalogConfig.SOURCE_ID
        val remoteId = catalogEpisode.id
        val resolvedUrl = CatalogConfig.resolveAudioUrl(catalogEpisode.audioUrl)

        val existing = episodes.value.find {
            it.sourceId == sourceId && it.remoteId == remoteId
        }
        if (existing != null) {
            val privateUri = downloadStore.uriForIdentity(DownloadIdentity(sourceId, remoteId))
            if (privateUri != null && existing.localUri == null) {
                scope.launch { dao.updateLocalUri(existing.id, privateUri.toString()) }
                return existing.copy(localUri = privateUri, audioUrl = resolvedUrl)
            }
            return existing.copy(audioUrl = resolvedUrl)
        }

        val privateUri = downloadStore.uriForIdentity(DownloadIdentity(sourceId, remoteId))
        return Episode(
            id = UUID.randomUUID().toString(),
            title = catalogEpisode.title,
            podcastName = podcast?.title ?: "Auralis",
            description = catalogEpisode.description,
            durationMs = catalogEpisode.durationMs,
            remoteId = remoteId,
            audioUrl = resolvedUrl,
            localUri = privateUri,
            isAiGenerated = catalogEpisode.isAiGenerated,
            sourceId = sourceId,
        )
    }

    suspend fun addToLibrary(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode = withContext(Dispatchers.IO) {
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, catalogEpisode.id)
        val resolvedUrl = CatalogConfig.resolveAudioUrl(catalogEpisode.audioUrl)
        val mutex = downloadMutexes.getOrPut(identity) { Mutex() }

        mutex.withLock {
            findCatalogEpisodeSuspend(identity.sourceId!!, identity.remoteId)?.let { existing ->
                return@withLock existing.copy(audioUrl = resolvedUrl)
            }

            val privateUri = downloadStore.uriForIdentity(identity)
            val episode = Episode(
                id = UUID.randomUUID().toString(),
                title = catalogEpisode.title,
                podcastName = podcast?.title ?: "Auralis",
                description = catalogEpisode.description,
                durationMs = catalogEpisode.durationMs,
                remoteId = identity.remoteId,
                audioUrl = resolvedUrl,
                localUri = privateUri,
                isAiGenerated = catalogEpisode.isAiGenerated,
                sourceId = identity.sourceId,
            )
            dao.upsert(EpisodeEntity.from(episode))
            episode
        }
    }

    /**
     * Removes only the durable Library membership for one exact remote identity.
     * A private download is a separate resource and is deliberately left intact.
     */
    suspend fun removeFromLibrary(identity: DownloadIdentity): Boolean = withContext(Dispatchers.IO) {
        val sourceId = identity.sourceId?.takeIf { it.isNotBlank() } ?: return@withContext false
        val mutex = downloadMutexes.getOrPut(identity) { Mutex() }

        mutex.withLock {
            val existing = dao.getBySourceAndRemoteId(sourceId, identity.remoteId)
                ?: return@withLock false
            dao.delete(existing.id)
            if (_currentEpisodeId.value == existing.id) {
                _currentEpisodeId.value = null
            }
            true
        }
    }

    fun isDownloaded(identity: DownloadIdentity): Boolean =
        downloadStore.hasLocalCopy(identity)

    fun cancelDownload(identity: DownloadIdentity) {
        downloadStore.cancel(identity)
    }

    fun clearDownloadError(identity: DownloadIdentity? = null) {
        _downloadErrors.update { errors -> if (identity == null) emptyMap() else errors - identity }
    }

    suspend fun downloadEpisodeOffline(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode? = downloadEpisodeOffline(
        DownloadIdentity(CatalogConfig.SOURCE_ID, catalogEpisode.id),
        catalogEpisode,
        podcast,
    )

    /** Downloads one remote episode under its explicit source identity. */
    suspend fun downloadEpisodeOffline(
        identity: DownloadIdentity,
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode? = withContext(Dispatchers.IO) {
        require(identity.remoteId == catalogEpisode.id) {
            "Download identity remoteId must match the catalog episode"
        }
        if (!identity.isSourceAware) {
            _downloadErrors.update { it +
                (identity to "منبع دانلود مشخص نیست؛ فایل Legacy به‌صورت خودکار ساخته نمی‌شود.") }
            return@withContext null
        }
        val url = CatalogConfig.resolveAudioUrl(catalogEpisode.audioUrl)
        if (url.isBlank()) {
            _downloadErrors.update { it + (identity to "آدرس فایل خالی است.") }
            return@withContext null
        }

        val mutex = downloadMutexes.getOrPut(identity) { Mutex() }
        mutex.withLock {
            // Re-check after acquiring the identity lock: concurrent duplicate requests
            // converge on the first completed file and never download/write it twice.
            if (downloadStore.hasLocalCopy(identity)) {
                val uri = downloadStore.uriForIdentity(identity)
                    ?: return@withLock null
                return@withLock upsertDownloaded(catalogEpisode, podcast, url, uri, identity)
            }

            _downloadErrors.update { it - identity }
            _downloadingIdentities.update { it + identity }
            _downloadProgress.update { it + (identity to 0f) }
            try {
                val fileUri = downloadStore.download(identity, url) { progress ->
                    _downloadProgress.update { it + (identity to progress) }
                }
                upsertDownloaded(catalogEpisode, podcast, url, fileUri, identity)
            } catch (_: DownloadCancelledException) {
                _downloadErrors.update { it - identity }
                null
            } catch (_: InsufficientSpaceException) {
                _downloadErrors.update { it +
                    (identity to "فضای ذخیره‌سازی کافی نیست.") }
                null
            } catch (_: Exception) {
                _downloadErrors.update { it +
                    (identity to "دانلود انجام نشد. اتصال را بررسی کنید و دوباره تلاش کنید.") }
                null
            } finally {
                _downloadingIdentities.update { it - identity }
                _downloadProgress.update { it - identity }
            }
        }
    }

    private suspend fun upsertDownloaded(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
        url: String,
        fileUri: Uri,
        identity: DownloadIdentity,
    ): Episode {
        val sourceId = identity.sourceId!!
        val remoteId = identity.remoteId

        val existing = findCatalogEpisodeSuspend(sourceId, remoteId)
        if (existing != null) {
            dao.updateLocalUri(existing.id, fileUri.toString())
            return existing.copy(localUri = fileUri, audioUrl = url)
        }

        val episode = Episode(
            id = UUID.randomUUID().toString(),
            title = catalogEpisode.title,
            podcastName = podcast?.title ?: "Auralis",
            description = catalogEpisode.description,
            durationMs = catalogEpisode.durationMs,
            remoteId = remoteId,
            audioUrl = url,
            localUri = fileUri,
            isAiGenerated = catalogEpisode.isAiGenerated,
            sourceId = sourceId,
        )
        dao.upsert(EpisodeEntity.from(episode))
        return episode
    }

    private suspend fun findCatalogEpisodeSuspend(sourceId: String, remoteId: String): Episode? {
        dao.getBySourceAndRemoteId(sourceId, remoteId)?.toEpisode()?.let { return it }
        return episodes.value.find {
            it.sourceId == sourceId && it.remoteId == remoteId
        }
    }

    fun updatePosition(episodeId: String, positionMs: Long) {
        scope.launch {
            dao.updatePosition(episodeId, positionMs, System.currentTimeMillis())
        }
    }

    fun deleteEpisode(episodeId: String) {
        scope.launch {
            // Removing Library membership must not delete the user's offline file.
            // The private download remains addressable by its stable source identity.
            dao.delete(episodeId)
            if (_currentEpisodeId.value == episodeId) {
                _currentEpisodeId.value = null
            }
        }
    }

    /**
     * Explicitly removes the private offline copy while keeping Library membership.
     * Only the exact source identity is deleted; ambiguous legacy files are not claimed
     * by a source-aware row.
     */
    fun deleteDownload(episodeId: String) {
        scope.launch {
            val entity = dao.getById(episodeId) ?: return@launch
            val sourceId = entity.sourceId?.takeIf { it.isNotBlank() } ?: return@launch
            val remoteId = entity.remoteId?.takeIf { it.isNotBlank() } ?: return@launch

            // Serialize explicit deletion with download/retry for the same identity.
            // This prevents deletion from clearing a live cancellation flag or racing
            // the final .part -> target rename.
            val identity = DownloadIdentity(sourceId, remoteId)
            val mutex = downloadMutexes.getOrPut(identity) { Mutex() }
            mutex.withLock {
                // Null-source rows address ambiguous legacy storage; never delete those files.
                // Keep the database URI unless the exact source-owned file is removed.
                if (downloadStore.deleteExact(identity)) {
                    dao.updateLocalUri(episodeId, null)
                }
            }
        }
    }

    /** Source-aware private download URI when a file exists for this episode identity. */
    fun privateDownloadUri(episode: Episode): Uri? {
        val remoteId = episode.remoteId?.takeIf { it.isNotBlank() } ?: return null
        return downloadStore.uriForIdentity(DownloadIdentity(episode.sourceId, remoteId))
    }

    fun setCurrentEpisode(id: String?) {
        _currentEpisodeId.value = id
    }

    fun getEpisode(id: String): Episode? = episodes.value.find { it.id == id }

    fun getCurrentEpisode(): Episode? =
        _currentEpisodeId.value?.let { id -> episodes.value.find { it.id == id } }
}
