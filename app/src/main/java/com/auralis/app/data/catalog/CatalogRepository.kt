package com.auralis.app.data.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Catalog access. Sample feed uses public HTTPS MP3s so streaming can be tested
 * before the real Auralis CDN is online.
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
        // Public test streams (HTTPS) — replace with Auralis CDN URLs when available.
        private const val STREAM_1 =
            "https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3"
        private const val STREAM_2 =
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        private const val STREAM_3 =
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        private const val STREAM_4 =
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"

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
                description = "نمونه استریم HTTPS برای تست پخش آنلاین.",
                audioUrl = STREAM_1,
                durationMs = 0L,
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-philo-2",
                podcastId = "ai-philosophy",
                title = "اخلاق تولید محتوا با AI",
                description = "نمونه استریم — SoundHelix 1.",
                audioUrl = STREAM_2,
                durationMs = 0L,
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-sci-1",
                podcastId = "ai-science",
                title = "سیاه‌چاله‌ها در ده دقیقه",
                description = "نمونه استریم — SoundHelix 2.",
                audioUrl = STREAM_3,
                durationMs = 0L,
                isAiGenerated = true,
            ),
            CatalogEpisode(
                id = "ep-tech-1",
                podcastId = "ai-tech",
                title = "مدل‌های زبانی چه چیزی را بلدند؟",
                description = "نمونه استریم — SoundHelix 3.",
                audioUrl = STREAM_4,
                durationMs = 0L,
                isAiGenerated = true,
            ),
        )
    }
}
