package com.auralis.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.auralis.app.data.Episode
import android.net.Uri

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val podcastName: String = "Auralis",
    val description: String = "",
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
    val localUri: String? = null,
    val remoteId: String? = null,
    val audioUrl: String? = null,
    val isAiGenerated: Boolean = true,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long? = null,
) {
    fun toEpisode(): Episode = Episode(
        id = id,
        title = title,
        podcastName = podcastName,
        description = description,
        durationMs = durationMs,
        artworkUri = artworkUri?.let { Uri.parse(it) },
        localUri = localUri?.let { Uri.parse(it) },
        remoteId = remoteId,
        audioUrl = audioUrl,
        isAiGenerated = isAiGenerated,
        lastPositionMs = lastPositionMs,
        lastPlayedAt = lastPlayedAt,
    )

    companion object {
        fun from(episode: Episode): EpisodeEntity = EpisodeEntity(
            id = episode.id,
            title = episode.title,
            podcastName = episode.podcastName,
            description = episode.description,
            durationMs = episode.durationMs,
            artworkUri = episode.artworkUri?.toString(),
            localUri = episode.localUri?.toString(),
            remoteId = episode.remoteId,
            audioUrl = episode.audioUrl,
            isAiGenerated = episode.isAiGenerated,
            lastPositionMs = episode.lastPositionMs,
            lastPlayedAt = episode.lastPlayedAt,
        )
    }
}
