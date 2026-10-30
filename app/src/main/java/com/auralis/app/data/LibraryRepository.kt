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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LibraryRepository internal constructor(
    private val dao: EpisodeDao,
    private val downloadStore: PrivateDownloadStore,
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

    private val _downloadingRemoteIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingRemoteIds: StateFlow<Set<String>> = _downloadingRemoteIds.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _lastDownloadError = MutableStateFlow<String?>(null)
    val lastDownloadError: StateFlow<String?> = _lastDownloadError.asStateFlow()

    private val downloadMutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun addLocalEpisode(
        uri: Uri,
        title: String,
        durationMs: Long = 0L,
        artworkUri: Uri? = null,
    ): Episode {
        val existing = dao.getByLocalUri(uri.toString())
        if (existing != null) return existing.toEpisode()

        val episode = Episode(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "اپیزود بدون عنوان" },
            podcastName = "Auralis",
            durationMs = durationMs,
            artworkUri = artworkUri,
            localUri = uri,
            isAiGenerated = true,
        )
        dao.upsert(EpisodeEntity.from(episode))
        return episode
    }

    fun addOrGetStreamEpisode(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode {
        val resolvedUrl = CatalogConfig.resolveAudioUrl(catalogEpisode.audioUrl)
        val existing = episodes.value.find { it.remoteId == catalogEpisode.id }
        if (existing != null) {
            val privateUri = downloadStore.uriForRemoteId(catalogEpisode.id)
            if (privateUri != null && existing.localUri == null) {
                scope.launch { dao.updateLocalUri(existing.id, privateUri.toString()) }
                return existing.copy(localUri = privateUri, audioUrl = resolvedUrl)
            }
            return existing.copy(audioUrl = resolvedUrl)
        }

        val privateUri = downloadStore.uriForRemoteId(catalogEpisode.id)
        val episode = Episode(
            id = UUID.randomUUID().toString(),
            title = catalogEpisode.title,
            podcastName = podcast?.title ?: "Auralis",
            description = catalogEpisode.description,
            durationMs = catalogEpisode.durationMs,
            remoteId = catalogEpisode.id,
            audioUrl = resolvedUrl,
            localUri = privateUri,
            isAiGenerated = catalogEpisode.isAiGenerated,
        )
        scope.launch { dao.upsert(EpisodeEntity.from(episode)) }
        return episode
    }

    fun isDownloaded(remoteId: String): Boolean = downloadStore.hasLocalCopy(remoteId)

    fun cancelDownload(remoteId: String) {
        downloadStore.cancel(remoteId)
    }

    fun clearDownloadError() {
        _lastDownloadError.value = null
    }

    /**
     * Download into private storage. Safe to call again (retry).
     * Concurrent calls for the same remoteId are serialized / ignored if already running.
     */
    suspend fun downloadEpisodeOffline(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode? = withContext(Dispatchers.IO) {
        val remoteId = catalogEpisode.id
        val url = CatalogConfig.resolveAudioUrl(catalogEpisode.audioUrl)
        if (url.isBlank()) {
            _lastDownloadError.value = "آدرس فایل خالی است."
            return@withContext null
        }

        if (remoteId in _downloadingRemoteIds.value) {
            return@withContext null
        }

        val mutex = downloadMutexes.getOrPut(remoteId) { Mutex() }
        mutex.withLock {
            if (downloadStore.hasLocalCopy(remoteId)) {
                val uri = downloadStore.uriForRemoteId(remoteId)!!
                return@withLock upsertDownloaded(catalogEpisode, podcast, url, uri)
            }

            _lastDownloadError.value = null
            _downloadingRemoteIds.value = _downloadingRemoteIds.value + remoteId
            _downloadProgress.value = _downloadProgress.value + (remoteId to 0f)
            try {
                val fileUri = downloadStore.download(remoteId, url) { p ->
                    _downloadProgress.value = _downloadProgress.value + (remoteId to p)
                }
                upsertDownloaded(catalogEpisode, podcast, url, fileUri)
            } catch (_: DownloadCancelledException) {
                _lastDownloadError.value = null
                null
            } catch (e: InsufficientSpaceException) {
                _lastDownloadError.value = "فضای ذخیره‌سازی کافی نیست."
                null
            } catch (_: Exception) {
                _lastDownloadError.value = "دانلود انجام نشد. اتصال را بررسی کنید و دوباره تلاش کنید."
                null
            } finally {
                _downloadingRemoteIds.value = _downloadingRemoteIds.value - remoteId
                _downloadProgress.value = _downloadProgress.value - remoteId
            }
        }
    }

    private suspend fun upsertDownloaded(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
        url: String,
        fileUri: Uri,
    ): Episode {
        val existing = dao.getByRemoteId(catalogEpisode.id)?.toEpisode()
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
            remoteId = catalogEpisode.id,
            audioUrl = url,
            localUri = fileUri,
            isAiGenerated = catalogEpisode.isAiGenerated,
        )
        dao.upsert(EpisodeEntity.from(episode))
        return episode
    }

    fun updatePosition(episodeId: String, positionMs: Long) {
        scope.launch {
            dao.updatePosition(episodeId, positionMs, System.currentTimeMillis())
        }
    }

    fun deleteEpisode(episodeId: String) {
        scope.launch {
            val entity = dao.getById(episodeId)
            entity?.remoteId?.let { downloadStore.delete(it) }
            dao.delete(episodeId)
            if (_currentEpisodeId.value == episodeId) {
                _currentEpisodeId.value = null
            }
        }
    }

    fun setCurrentEpisode(id: String?) {
        _currentEpisodeId.value = id
    }

    fun getEpisode(id: String): Episode? = episodes.value.find { it.id == id }

    fun getCurrentEpisode(): Episode? =
        _currentEpisodeId.value?.let { id -> episodes.value.find { it.id == id } }
}
