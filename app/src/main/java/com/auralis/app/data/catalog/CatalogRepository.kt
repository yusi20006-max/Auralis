package com.auralis.app.data.catalog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Injectable boundary for deterministic CatalogRepository tests. */
fun interface CatalogHttpClient {
    fun get(url: String): String
}

/** Production HTTP implementation; timeouts are configurable for local-server tests. */
class UrlConnectionCatalogHttpClient(
    private val connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
) : CatalogHttpClient {

    init {
        require(connectTimeoutMs > 0)
        require(readTimeoutMs > 0)
    }

    override fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw CatalogHttpException(code)
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 12_000
    }
}

class CatalogHttpException(val statusCode: Int) : IllegalStateException("HTTP " + statusCode)

/**
 * The built-in sample remains available as initial/offline content. A failed
 * refresh never replaces the last known lists and is always reported as Error.
 */
class CatalogRepository(
    private val httpClient: CatalogHttpClient = UrlConnectionCatalogHttpClient(),
) {
    private val podcasts = MutableStateFlow(SAMPLE_PODCASTS)
    private val episodes = MutableStateFlow(SAMPLE_EPISODES)
    private val _loadState = MutableStateFlow<CatalogLoadState>(CatalogLoadState.Idle)
    val loadState = _loadState.asStateFlow()

    fun observePodcasts(): Flow<List<CatalogPodcast>> = podcasts.asStateFlow()

    fun observePodcasts(category: String): Flow<List<CatalogPodcast>> =
        podcasts.map { list ->
            if (category == "همه") list else list.filter { it.category == category }
        }

    fun observeEpisodes(podcastId: String? = null): Flow<List<CatalogEpisode>> {
        if (podcastId == null) return episodes.asStateFlow()
        return episodes.map { list -> list.filter { it.podcastId == podcastId } }
    }

    fun getPodcast(id: String): CatalogPodcast? = podcasts.value.find { it.id == id }
    fun getEpisode(id: String): CatalogEpisode? = episodes.value.find { it.id == id }

    /** Fetch, parse, and atomically replace the in-memory catalog only on success. */
    suspend fun refresh(): CatalogLoadState = withContext(Dispatchers.IO) {
        val url = CatalogConfig.catalogUrl.trim()
        if (url.isEmpty()) {
            val state = CatalogLoadState.Skipped("catalogUrl empty")
            _loadState.value = state
            return@withContext state
        }

        _loadState.value = CatalogLoadState.Loading
        try {
            val parsed = CatalogJsonParser.parse(httpClient.get(url))
            if (parsed.podcasts.isEmpty()) {
                val state = CatalogLoadState.Error("empty catalog")
                _loadState.value = state
                return@withContext state
            }

            podcasts.value = parsed.podcasts
            episodes.value = parsed.episodes
            val state = CatalogLoadState.Success(parsed.podcasts.size, parsed.episodes.size)
            _loadState.value = state
            state
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            // Do not expose raw URLs, response bodies, or arbitrary server text to UI state.
            val detail = (failure as? CatalogHttpException)?.message
                ?: "Catalog request or parsing failed (" + failure.javaClass.simpleName + ")"
            val state = CatalogLoadState.Error(detail)
            _loadState.value = state
            state
        }
    }

    companion object {
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

sealed class CatalogLoadState {
    data object Idle : CatalogLoadState()
    data object Loading : CatalogLoadState()
    data class Success(val podcastCount: Int, val episodeCount: Int) : CatalogLoadState()
    data class Error(val message: String) : CatalogLoadState()
    data class Skipped(val reason: String) : CatalogLoadState()
}
