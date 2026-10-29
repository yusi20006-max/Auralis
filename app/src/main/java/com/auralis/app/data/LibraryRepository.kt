package com.auralis.app.data

import android.content.Context
import android.net.Uri
import com.auralis.app.data.db.AuralisDatabase
import com.auralis.app.data.db.EpisodeEntity
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
import java.util.UUID

/**
 * Library repository backed by Room.
 * UI observes [episodes] and [currentEpisodeId] as StateFlows.
 */
class LibraryRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = AuralisDatabase.get(context).episodeDao()

    val episodes: StateFlow<List<Episode>> = dao.observeAll()
        .map { list -> list.map { it.toEpisode() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentEpisodeId = MutableStateFlow<String?>(null)
    val currentEpisodeId: StateFlow<String?> = _currentEpisodeId.asStateFlow()

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

    fun updatePosition(episodeId: String, positionMs: Long) {
        scope.launch {
            dao.updatePosition(episodeId, positionMs, System.currentTimeMillis())
        }
    }

    fun deleteEpisode(episodeId: String) {
        scope.launch {
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
