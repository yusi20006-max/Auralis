package com.auralis.app.data

import android.net.Uri

/**
 * Core episode model for Auralis.
 * Local-first: [localUri] is the primary source for the offline MVP.
 * Future remote catalog can fill [remoteId] / [audioUrl] without breaking local playback.
 */
data class Episode(
    val id: String,
    val title: String,
    val podcastName: String = "Auralis",
    val description: String = "",
    val durationMs: Long = 0L,
    val artworkUri: Uri? = null,
    val localUri: Uri? = null,
    val remoteId: String? = null,
    val audioUrl: String? = null,
    val isAiGenerated: Boolean = true,
    val lastPositionMs: Long = 0L,
    val lastPlayedAt: Long? = null,
) {
    val hasLocalSource: Boolean get() = localUri != null
    val progressFraction: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
