package com.auralis.app.data.catalog

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {

    private val repository = CatalogRepository()

    @Test
    fun observePodcastsReturnsSeededCatalog() = runBlocking {
        val podcasts = repository.observePodcasts().first()
        assertTrue(podcasts.isNotEmpty())
        assertTrue(podcasts.all { it.id.isNotBlank() && it.title.isNotBlank() })
    }

    @Test
    fun observePodcastsFiltersByCategory() = runBlocking {
        val philosophy = repository.observePodcasts("فلسفه").first()
        assertTrue(philosophy.isNotEmpty())
        assertTrue(philosophy.all { it.category == "فلسفه" })

        val all = repository.observePodcasts("همه").first()
        assertTrue(all.size >= philosophy.size)
    }

    @Test
    fun observeEpisodesForPodcastReturnsOnlyMatchingRows() = runBlocking {
        val episodes = repository.observeEpisodes("ai-philosophy").first()
        assertTrue(episodes.isNotEmpty())
        assertTrue(episodes.all { it.podcastId == "ai-philosophy" })
        assertTrue(episodes.all { it.audioUrl.startsWith("http") })
    }

    @Test
    fun getPodcastAndEpisodeLookup() {
        assertNotNull(repository.getPodcast("ai-science"))
        assertNull(repository.getPodcast("missing-podcast"))
        assertNotNull(repository.getEpisode("ep-sci-1"))
        assertNull(repository.getEpisode("missing-episode"))
    }

    @Test
    fun catalogSourceIdIsStableForHostedCatalog() {
        assertEquals("catalog:auralis", CatalogConfig.SOURCE_ID)
    }

    @Test
    fun resolveAudioUrlKeepsAbsoluteHttps() {
        val absolute = "https://example.com/a.mp3"
        assertEquals(absolute, CatalogConfig.resolveAudioUrl(absolute))
    }

    @Test
    fun resolveAudioUrlJoinsRelativePathWhenBaseConfigured() {
        val previous = CatalogConfig.baseUrl
        try {
            CatalogConfig.baseUrl = "https://cdn.example/"
            assertEquals(
                "https://cdn.example/audio/ep1.mp3",
                CatalogConfig.resolveAudioUrl("/audio/ep1.mp3"),
            )
        } finally {
            CatalogConfig.baseUrl = previous
        }
    }
}
