package com.auralis.app.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogJsonParserTest {

    @Test
    fun parseValidCatalogJson() {
        val json = """
        {
          "version": 1,
          "podcasts": [
            {
              "id": "p1",
              "title": "Test Pod",
              "description": "desc",
              "episodeCount": 1,
              "category": "علم",
              "isAiGenerated": true
            }
          ],
          "episodes": [
            {
              "id": "e1",
              "podcastId": "p1",
              "title": "Ep 1",
              "audioUrl": "https://example.com/a.mp3",
              "durationMs": 12000,
              "isAiGenerated": true
            }
          ]
        }
        """.trimIndent()

        val parsed = CatalogJsonParser.parse(json)
        assertEquals(1, parsed.podcasts.size)
        assertEquals("p1", parsed.podcasts[0].id)
        assertEquals("علم", parsed.podcasts[0].category)
        assertEquals(1, parsed.episodes.size)
        assertEquals("https://example.com/a.mp3", parsed.episodes[0].audioUrl)
        assertEquals(12000L, parsed.episodes[0].durationMs)
    }

    @Test
    fun skipsRowsMissingRequiredFields() {
        val json = """
        {
          "podcasts": [
            { "id": "", "title": "x" },
            { "id": "ok", "title": "Ok" }
          ],
          "episodes": [
            { "id": "e", "podcastId": "ok", "title": "t", "audioUrl": "" }
          ]
        }
        """.trimIndent()
        val parsed = CatalogJsonParser.parse(json)
        assertEquals(1, parsed.podcasts.size)
        assertTrue(parsed.episodes.isEmpty())
    }
}
