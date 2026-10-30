package com.auralis.app.data

import android.content.Context
import android.net.Uri
import com.auralis.app.data.catalog.CatalogEpisode
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.db.AuralisDatabase
import com.auralis.app.data.db.EpisodeEntity
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
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Library repository backed by Room + private offline downloads.
 */
class LibraryRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = AuralisDatabase.get(context).episodeDao()
    private val downloadStore = PrivateDownloadStore(context)

    val episodes: StateFlow<List<Episode>> = dao.observeAll()
        .map { list -> list.map { it.toEpisode() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentEpisodeId = MutableStateFlow<String?>(null)
    val currentEpisodeId: StateFlow<String?> = _currentEpisodeId.asStateFlow()

    /** remoteId → true while download is in progress */
    private val _downloadingRemoteIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingRemoteIds: StateFlow<Set<String>> = _downloadingRemoteIds.asStateFlow()

    fun addLocalEpisode(
        uri: Uri,
        title: String,
        durationMs: Long = 0L,
        artworkUri: Uri? = null,
    ): Episode {
        val episode = Episode(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "اپیزود بدون عنوان" },
            podcastName = "Auralis",
            durationMs = durationMs,
            artworkUri = artworkUri,
            localUri = uri,
            isAiGenerated = true,
        )
        scope.launch {
            val existing = episodes.value.find { it.localUri == uri }
            if (existing == null) {
                dao.upsert(EpisodeEntity.from(episode))
            }
        }
        return episode
    }

    fun addOrGetStreamEpisode(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode {
        val existing = episodes.value.find { it.remoteId == catalogEpisode.id }
        if (existing != null) {
            // Prefer private file if already downloaded
            val privateUri = downloadStore.uriForRemoteId(catalogEpisode.id)
            if (privateUri != null && existing.localUri == null) {
                scope.launch {
                    dao.updateLocalUri(existing.id, privateUri.toString())
                }
                return existing.copy(localUri = privateUri)
            }
            return existing
        }

        val privateUri = downloadStore.uriForRemoteId(catalogEpisode.id)
        val episode = Episode(
            id = UUID.randomUUID().toString(),
            title = catalogEpisode.title,
            podcastName = podcast?.title ?: "Auralis",
            description = catalogEpisode.description,
            durationMs = catalogEpisode.durationMs,
            remoteId = catalogEpisode.id,
            audioUrl = catalogEpisode.audioUrl,
            localUri = privateUri,
            isAiGenerated = catalogEpisode.isAiGenerated,
        )
        scope.launch {
            dao.upsert(EpisodeEntity.from(episode))
        }
        return episode
    }

    fun isDownloaded(remoteId: String): Boolean = downloadStore.hasLocalCopy(remoteId)

    /**
     * Download catalog audio into app-private storage (not visible to file managers).
     * @return updated [Episode] with localUri set, or null on failure.
     */
    suspend fun downloadEpisodeOffline(
        catalogEpisode: CatalogEpisode,
        podcast: CatalogPodcast?,
    ): Episode? = withContext(Dispatchers.IO) {
        val remoteId = catalogEpisode.id
        val url = catalogEpisode.audioUrl
        if (url.isBlank()) return@withContext null

        _downloadingRemoteIds.value = _downloadingRemoteIds.value + remoteId
        try {
            val fileUri = if (downloadStore.hasLocalCopy(remoteId)) {
                downloadStore.uriForRemoteId(remoteId)!!
            } else {
                downloadStore.download(remoteId, url)
            }

            val existing = dao.getByRemoteId(remoteId)?.toEpisode()
            if (existing != null) {
                dao.updateLocalUri(existing.id, fileUri.toString())
                return@withContext existing.copy(localUri = fileUri)
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
            )
            dao.upsert(EpisodeEntity.from(episode))
            episode
        } catch (_: Exception) {
            null
        } finally {
            _downloadingRemoteIds.value = _downloadingRemoteIds.value - remoteId
        }
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
