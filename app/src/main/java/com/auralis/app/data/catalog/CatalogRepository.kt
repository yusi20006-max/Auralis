package com.auralis.app.data.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Catalog access. MVP uses an in-memory sample feed so Explore is not empty
 * while the real API is not wired yet.
 */
class CatalogRepository {

    private val podcasts = MutableStateFlow(SAMPLE_PODCASTS)
    private val episodes = MutableStateFlow(SAMPLE_EPISODES)

    fun observePodcasts(): Flow<List<CatalogPodcast>> = podcasts.asStateFlow()

    fun observePodcasts(category: String): Flow<List<CatalogPodcast>> =
        podcasts.map { list ->
            if (category == "همه") list
            else list.filter { it.category == category }
        }

    fun observeEpisodes(podcastId: String? = null): Flow<List<CatalogEpisode>> {
        if (podcastId == null) return episodes.asStateFlow()
        return episodes.map { list -> list.filter { it.podcastId == podcastId } }
    }

    fun getPodcast(id: String): CatalogPodcast? = podcasts.value.find { it.id == id }

    fun getEpisode(id: String): CatalogEpisode? = episodes.value.find { it.id == id }

    companion object {
        private val SAMPLE_PODCASTS = listOf(
            CatalogPodcast(
                id = "ai-philosophy",
                title = "فلسفه با هوش مصنوعی",
                description = "گفت‌وگوهای کوتاه تولیدشده با AI درباره معنا، اخلاق و آگاهی.",
                isAiGenerated = true,
                episodeCount = 2,
                category = "فلسفه",
            ),
            CatalogPodcast(
                id = "ai-science",
                title = "علم به زبان ساده",
                description = "اپیزودهای علمی کوتاه با روایت AI.",
                isAiGenerated = true,
                episodeCount = 1,
                category = "علم",
            ),
            CatalogPodcast(
                id = "ai-tech",
                title = "مرزهای تکنولوژی",
                description = "نگاهی به آینده ابزارها و مدل‌های زبانی.",
                isAiGenerated = true,
                episodeCount = 1,
                category = "تکنولوژی",
            ),
        )

        private val SAMPLE_EPISODES = listOf(
            CatalogEpisode(
                id = "ep-philo-1",
                podcastId = "ai-philosophy",
                title = "آیا ماشین می‌تواند بفهمد؟",
                description = "نمونه‌ی کاتالوگ — فایل صوتی هنوز به سرور وصل نیست.",
                audioUrl = "",
                durationMs = 12 * 60_000L,
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-philo-2",
                podcastId = "ai-philosophy",
                title = "اخلاق تولید محتوا با AI",
                description = "نمونه‌ی کاتالوگ — استریم در فاز بعد.",
                audioUrl = "",
                durationMs = 15 * 60_000L,
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-sci-1",
                podcastId = "ai-science",
                title = "سیاه‌چاله‌ها در ده دقیقه",
                description = "نمونه‌ی کاتالوگ.",
                audioUrl = "",
                durationMs = 10 * 60_000L,
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-tech-1",
                podcastId = "ai-tech",
                title = "مدل‌های زبانی چه چیزی را بلدند؟",
                description = "نمونه‌ی کاتالوگ.",
                audioUrl = "",
                durationMs = 18 * 60_000L,
                isAiGenerated = true,
            ),
        )
    }
}
