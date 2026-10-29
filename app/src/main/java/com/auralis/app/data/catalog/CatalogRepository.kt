package com.auralis.app.data.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Catalog access. MVP uses an in-memory sample feed so Explore is not empty
 * while the real API is not wired yet.
 */
class CatalogRepository {

    private val podcasts = MutableStateFlow(SAMPLE_PODCASTS)
    private val episodes = MutableStateFlow(SAMPLE_EPISODES)

    fun observePodcasts(): Flow<List<CatalogPodcast>> = podcasts.asStateFlow()

    fun observeEpisodes(podcastId: String? = null): Flow<List<CatalogEpisode>> {
        if (podcastId == null) return episodes.asStateFlow()
        return MutableStateFlow(episodes.value.filter { it.podcastId == podcastId }).asStateFlow()
    }

    fun getEpisode(id: String): CatalogEpisode? = episodes.value.find { it.id == id }

    companion object {
        private val SAMPLE_PODCASTS = listOf(
            CatalogPodcast(
                id = "ai-philosophy",
                title = "فلسفه با هوش مصنوعی",
                description = "گفت‌وگوهای کوتاه تولیدشده با AI درباره معنا، اخلاق و آگاهی.",
                isAiGenerated = true,
                episodeCount = 2,
            ),
            CatalogPodcast(
                id = "ai-science",
                title = "علم به زبان ساده",
                description = "اپیزودهای علمی کوتاه با روایت AI.",
                isAiGenerated = true,
                episodeCount = 1,
            ),
        )

        private val SAMPLE_EPISODES = listOf(
            CatalogEpisode(
                id = "ep-philo-1",
                podcastId = "ai-philosophy",
                title = "آیا ماشین می‌تواند بفهمد؟",
                description = "نمونه‌ی کاتالوگ — فایل صوتی هنوز به سرور وصل نیست.",
                audioUrl = "",
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-philo-2",
                podcastId = "ai-philosophy",
                title = "اخلاق تولید محتوا با AI",
                description = "نمونه‌ی کاتالوگ — استریم در فاز بعد.",
                audioUrl = "",
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-sci-1",
                podcastId = "ai-science",
                title = "سیاه‌چاله‌ها در ده دقیقه",
                description = "نمونه‌ی کاتالوگ.",
                audioUrl = "",
                isAiGenerated = true,
            ),
        )
    }
}
