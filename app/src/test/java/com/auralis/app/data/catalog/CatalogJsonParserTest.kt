package com.auralis.app.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Avoids Robolectric/org.json on the pure JVM classpath.
 * Full JSON parse is covered on-device; here we lock config + sample seed.
 */
class CatalogJsonParserTest {

    @Test
    fun catalogUrlIsHttpsJsonEndpoint() {
        assertTrue(CatalogConfig.catalogUrl.startsWith("https://"))
        assertTrue(CatalogConfig.catalogUrl.endsWith("catalog.json"))
    }

    @Test
    fun sampleRepositoryStillHasPhilosophyPodcast() {
        val repo = CatalogRepository()
        val podcast = repo.getPodcast("ai-philosophy")
        assertTrue(podcast != null)
        assertEquals("ai-philosophy", podcast!!.id)
    }

    @Test
    fun resolveAudioUrlAbsolutePassthrough() {
        val url = "https://cdn.example/a.mp3"
        assertEquals(url, CatalogConfig.resolveAudioUrl(url))
    }
}
