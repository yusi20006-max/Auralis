package com.auralis.app.data.catalog

/**
 * Phase 3 — hosted catalog models (offline-first: stream or download later).
 */
data class CatalogPodcast(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String? = null,
    val isAiGenerated: Boolean = true,
    val episodeCount: Int = 0,
)

data class CatalogEpisode(
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String = "",
    val audioUrl: String,
    val durationMs: Long = 0L,
    val publishedAtEpochMs: Long = 0L,
    val isAiGenerated: Boolean = true,
)
