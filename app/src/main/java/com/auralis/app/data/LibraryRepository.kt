package com.auralis.app.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Simple in-memory library for the offline MVP.
 * Can be replaced with Room later without changing the UI layer.
 */
class LibraryRepository {

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

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
        _episodes.update { current ->
            // Avoid exact duplicate URIs
            if (current.any { it.localUri == uri }) current else listOf(episode) + current
        }
        return episode
    }

    fun updatePosition(episodeId: String, positionMs: Long) {
        _episodes.update { list ->
            list.map {
                if (it.id == episodeId) it.copy(
                    lastPositionMs = positionMs,
                    lastPlayedAt = System.currentTimeMillis()
                ) else it
            }
        }
    }

    fun setCurrentEpisode(id: String?) {
        _currentEpisodeId.value = id
    }

    fun getEpisode(id: String): Episode? = _episodes.value.find { it.id == id }

    fun getCurrentEpisode(): Episode? =
        _currentEpisodeId.value?.let { id -> _episodes.value.find { it.id == id } }
}
