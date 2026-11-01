package com.auralis.app.data.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CatalogJsonParserTest {

    @Test
    fun parseMapsPodcastAndEpisodeFieldsFromRealJson() {
        val previousBase = CatalogConfig.baseUrl
        CatalogConfig.baseUrl = ""
        try {
            val parsed = CatalogJsonParser.parse(
                """
                {
                  "version": 1,
                  "podcasts": [{
                    "id": "science",
                    "title": "Science",
                    "description": "A podcast",
                    "coverUrl": "https://cdn.example/cover.jpg",
                    "isAiGenerated": false,
                    "episodeCount": 3,
                    "category": "علم"
                  }],
                  "episodes": [{
                    "id": "episode-1",
                    "podcastId": "science",
                    "title": "First",
                    "description": "Episode description",
                    "audioUrl": "https://cdn.example/audio.mp3",
                    "durationMs": 125000,
                    "publishedAtEpochMs": 1700000000000,
                    "isAiGenerated": false
                  }]
                }
                """.trimIndent(),
            )

            assertEquals(1, parsed.podcasts.size)
            assertEquals(
                CatalogPodcast(
                    id = "science",
                    title = "Science",
                    description = "A podcast",
                    coverUrl = "https://cdn.example/cover.jpg",
                    isAiGenerated = false,
                    episodeCount = 3,
                    category = "علم",
                ),
                parsed.podcasts.single(),
            )
            assertEquals(
                CatalogEpisode(
                    id = "episode-1",
                    podcastId = "science",
                    title = "First",
                    description = "Episode description",
                    audioUrl = "https://cdn.example/audio.mp3",
                    durationMs = 125000,
                    publishedAtEpochMs = 1700000000000,
                    isAiGenerated = false,
                ),
                parsed.episodes.single(),
            )
        } finally {
            CatalogConfig.baseUrl = previousBase
        }
    }

    @Test
    fun optionalNullFieldsUseDocumentedDefaults() {
        val parsed = CatalogJsonParser.parse(
            """
            {
              "podcasts": [{
                "id": "p1",
                "title": "Podcast",
                "description": null,
                "coverUrl": null,
                "isAiGenerated": null,
                "episodeCount": null,
                "category": null
              }],
              "episodes": [{
                "id": "e1",
                "podcastId": "p1",
                "title": "Episode",
                "description": null,
                "audioUrl": "https://cdn.example/e1.mp3",
                "durationMs": null,
                "publishedAtEpochMs": null,
                "isAiGenerated": null
              }]
            }
            """.trimIndent(),
        )

        assertEquals("", parsed.podcasts.single().description)
        assertNull(parsed.podcasts.single().coverUrl)
        assertTrue(parsed.podcasts.single().isAiGenerated)
        assertEquals(0, parsed.podcasts.single().episodeCount)
        assertEquals("همه", parsed.podcasts.single().category)
        assertEquals("", parsed.episodes.single().description)
        assertEquals(0L, parsed.episodes.single().durationMs)
        assertEquals(0L, parsed.episodes.single().publishedAtEpochMs)
        assertTrue(parsed.episodes.single().isAiGenerated)
    }

    @Test
    fun malformedRowsAndInvalidMediaUrlsAreSkippedWithoutCoercion() {
        val previousBase = CatalogConfig.baseUrl
        CatalogConfig.baseUrl = ""
        try {
            val parsed = CatalogJsonParser.parse(
                """
                {
                  "podcasts": [
                    {"id": "", "title": "Missing id"},
                    {"id": "bad-type", "title": 42},
                    {"id": "bad-optional", "title": "Bad optional", "episodeCount": "many"},
                    {"id": "valid", "title": "Valid", "coverUrl": "javascript:alert(1)"}
                  ],
                  "episodes": [
                    {"id": "no-audio", "podcastId": "p", "title": "No audio", "audioUrl": ""},
                    {"id": "bad-scheme", "podcastId": "p", "title": "Bad scheme", "audioUrl": "file:///tmp/a.mp3"},
                    {"id": "relative", "podcastId": "p", "title": "Relative", "audioUrl": "audio/a.mp3"},
                    {"id": "bad-duration", "podcastId": "p", "title": "Bad duration", "audioUrl": "https://cdn.example/a.mp3", "durationMs": -1},
                    {"id": "valid-e", "podcastId": "p", "title": "Valid", "audioUrl": "https://cdn.example/a.mp3"}
                  ]
                }
                """.trimIndent(),
            )

            assertEquals(listOf("valid"), parsed.podcasts.map { it.id })
            assertNull(parsed.podcasts.single().coverUrl)
            assertEquals(listOf("valid-e"), parsed.episodes.map { it.id })
        } finally {
            CatalogConfig.baseUrl = previousBase
        }
    }

    @Test
    fun malformedJsonAndWrongRootFieldTypesFailTheParse() {
        assertThrows(JSONException::class.java) {
            CatalogJsonParser.parse("{not-json")
        }
        assertThrows(JSONException::class.java) {
            CatalogJsonParser.parse("""{"podcasts":"not-an-array","episodes":[]}""")
        }
    }

    @Test
    fun urlResolutionAllowsOnlyHttpAndHttpsAndRequiresBaseForRelativePaths() {
        val previousBase = CatalogConfig.baseUrl
        try {
            CatalogConfig.baseUrl = ""
            assertEquals("https://cdn.example/a.mp3", CatalogConfig.resolveAudioUrl("https://cdn.example/a.mp3"))
            assertEquals("", CatalogConfig.resolveAudioUrl("file:///tmp/a.mp3"))
            assertEquals("", CatalogConfig.resolveAudioUrl("audio/a.mp3"))
            assertNull(CatalogConfig.resolveArtworkUrl("javascript:alert(1)"))

            CatalogConfig.baseUrl = "https://cdn.example/media/"
            assertEquals(
                "https://cdn.example/media/audio/a.mp3",
                CatalogConfig.resolveAudioUrl("/audio/a.mp3"),
            )
            assertEquals(
                "https://cdn.example/media/covers/a.jpg",
                CatalogConfig.resolveArtworkUrl("covers/a.jpg"),
            )
        } finally {
            CatalogConfig.baseUrl = previousBase
        }
    }

    @Test
    fun emptyOrNullArraysAreTreatedAsEmptyCollections() {
        val parsed = CatalogJsonParser.parse("""{"podcasts":null,"episodes":[]}""")
        assertTrue(parsed.podcasts.isEmpty())
        assertTrue(parsed.episodes.isEmpty())
    }
}
