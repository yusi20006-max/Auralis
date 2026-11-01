package com.auralis.app.data.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CatalogRepositoryTest {

    @Test
    fun refreshUsesRealHttpRequestAndReplacesSampleCatalogOnSuccess() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        LocalHttpServer(response = httpResponse(200, validCatalogJson())).use { server ->
            try {
                CatalogConfig.catalogUrl = server.url
                val repository = CatalogRepository(UrlConnectionCatalogHttpClient(1_000, 1_000))

                val result = repository.refresh()

                assertEquals(CatalogLoadState.Success(1, 1), result)
                assertEquals(result, repository.loadState.value)
                assertNull(repository.getPodcast("ai-philosophy"))
                assertEquals("remote-podcast", repository.getPodcast("remote-podcast")?.id)
                assertEquals("remote-episode", repository.getEpisode("remote-episode")?.id)
                assertEquals(
                    "https://cdn.example/audio.mp3",
                    repository.getEpisode("remote-episode")?.audioUrl,
                )
            } finally {
                CatalogConfig.catalogUrl = previousUrl
            }
        }
    }

    @Test
    fun httpClientSendsJsonAcceptHeaderAndUsesConfiguredEndpoint() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        LocalHttpServer(response = httpResponse(200, validCatalogJson())).use { server ->
            try {
                CatalogConfig.catalogUrl = server.url
                val repository = CatalogRepository(UrlConnectionCatalogHttpClient(1_000, 1_000))
                assertTrue(repository.refresh() is CatalogLoadState.Success)
                assertTrue(server.requestLine.startsWith("GET /catalog.json?key=not-a-secret HTTP/1.1"))
                assertTrue(server.requestHeaders.contains("Accept: application/json"))
            } finally {
                CatalogConfig.catalogUrl = previousUrl
            }
        }
    }

    @Test
    fun httpClient4xxAnd5xxAreErrorsAndDoNotReplaceCachedCatalog() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        try {
            for (statusCode in listOf(404, 503)) {
                LocalHttpServer(response = httpResponse(statusCode, """{"error":"upstream detail"}""")).use { server ->
                    CatalogConfig.catalogUrl = server.url
                    val repository = CatalogRepository(UrlConnectionCatalogHttpClient(1_000, 1_000))
                    val beforePodcasts = repository.observePodcasts().first()
                    val beforeEpisodes = repository.observeEpisodes().first()

                    val result = repository.refresh()

                    assertEquals(CatalogLoadState.Error("HTTP " + statusCode), result)
                    assertEquals(beforePodcasts, repository.observePodcasts().first())
                    assertEquals(beforeEpisodes, repository.observeEpisodes().first())
                    assertEquals(result, repository.loadState.value)
                    val error = result as CatalogLoadState.Error
                    assertFalse(error.message.contains("upstream detail"))
                    assertFalse(error.message.contains("not-a-secret"))
                }
            }
        } finally {
            CatalogConfig.catalogUrl = previousUrl
        }
    }

    @Test
    fun failedRefreshAfterRemoteSuccessKeepsLastKnownGoodCatalog() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        try {
            val repository = CatalogRepository(UrlConnectionCatalogHttpClient(1_000, 1_000))
            LocalHttpServer(response = httpResponse(200, validCatalogJson())).use { successServer ->
                CatalogConfig.catalogUrl = successServer.url
                assertEquals(CatalogLoadState.Success(1, 1), repository.refresh())
            }

            val cachedPodcast = repository.getPodcast("remote-podcast")
            val cachedEpisode = repository.getEpisode("remote-episode")
            LocalHttpServer(response = httpResponse(503, """{"error":"unavailable"}""")).use { failedServer ->
                CatalogConfig.catalogUrl = failedServer.url
                val result = repository.refresh()

                assertEquals(CatalogLoadState.Error("HTTP 503"), result)
                assertEquals(cachedPodcast, repository.getPodcast("remote-podcast"))
                assertEquals(cachedEpisode, repository.getEpisode("remote-episode"))
                assertNull(repository.getPodcast("ai-philosophy"))
            }
        } finally {
            CatalogConfig.catalogUrl = previousUrl
        }
    }

    @Test
    fun readTimeoutIsReportedAndPreviousCatalogRemainsAvailable() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        LocalHttpServer(holdOpenMillis = 500).use { server ->
            try {
                CatalogConfig.catalogUrl = server.url
                val repository = CatalogRepository(UrlConnectionCatalogHttpClient(500, 100))
                val before = repository.observePodcasts().first()

                val result = repository.refresh()

                assertTrue(result is CatalogLoadState.Error)
                assertTrue((result as CatalogLoadState.Error).message.contains("SocketTimeoutException"))
                assertEquals(before, repository.observePodcasts().first())
                assertNotNull(repository.getPodcast("ai-philosophy"))
            } finally {
                CatalogConfig.catalogUrl = previousUrl
            }
        }
    }

    @Test
    fun disconnectedSocketIsReportedAndDoesNotEraseCatalog() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        LocalHttpServer(disconnectWithoutResponse = true).use { server ->
            try {
                CatalogConfig.catalogUrl = server.url
                val repository = CatalogRepository(UrlConnectionCatalogHttpClient(500, 500))
                val before = repository.observeEpisodes().first()

                val result = repository.refresh()

                assertTrue(result is CatalogLoadState.Error)
                assertEquals(before, repository.observeEpisodes().first())
                assertNotNull(repository.getEpisode("ep-philo-1"))
            } finally {
                CatalogConfig.catalogUrl = previousUrl
            }
        }
    }

    @Test
    fun emptyAndMalformedSuccessfulResponsesAreErrorsNotRemoteSuccess() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        try {
            for (body in listOf("", "{broken-json")) {
                LocalHttpServer(response = httpResponse(200, body)).use { server ->
                    CatalogConfig.catalogUrl = server.url
                    val repository = CatalogRepository(UrlConnectionCatalogHttpClient(1_000, 1_000))
                    val before = repository.observePodcasts().first()

                    val result = repository.refresh()

                    assertTrue(result is CatalogLoadState.Error)
                    assertEquals(before, repository.observePodcasts().first())
                    assertNotNull(repository.getPodcast("ai-philosophy"))
                }
            }
        } finally {
            CatalogConfig.catalogUrl = previousUrl
        }
    }

    @Test
    fun validHttpResponseWithNoPodcastsIsAnErrorAndKeepsCache() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        LocalHttpServer(response = httpResponse(200, """{"podcasts":[],"episodes":[]}""")).use { server ->
            try {
                CatalogConfig.catalogUrl = server.url
                val repository = CatalogRepository(UrlConnectionCatalogHttpClient(1_000, 1_000))
                val before = repository.observePodcasts().first()

                val result = repository.refresh()

                assertEquals(CatalogLoadState.Error("empty catalog"), result)
                assertEquals(before, repository.observePodcasts().first())
            } finally {
                CatalogConfig.catalogUrl = previousUrl
            }
        }
    }

    @Test
    fun blankEndpointIsSkippedWithoutClaimingRemoteSuccess() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        try {
            CatalogConfig.catalogUrl = "  "
            val repository = CatalogRepository(CatalogHttpClient { error("HTTP must not be called") })

            val result = repository.refresh()

            assertEquals(CatalogLoadState.Skipped("catalogUrl empty"), result)
            assertNotNull(repository.getPodcast("ai-philosophy"))
        } finally {
            CatalogConfig.catalogUrl = previousUrl
        }
    }

    @Test
    fun injectedTransportCanVerifyRefreshWithoutPublicNetwork() = runBlocking {
        val previousUrl = CatalogConfig.catalogUrl
        try {
            CatalogConfig.catalogUrl = "https://catalog.invalid/catalog.json"
            var requestedUrl: String? = null
            val repository = CatalogRepository(CatalogHttpClient { url ->
                requestedUrl = url
                validCatalogJson()
            })

            val result = repository.refresh()

            assertEquals(CatalogLoadState.Success(1, 1), result)
            assertEquals(CatalogConfig.catalogUrl, requestedUrl)
        } finally {
            CatalogConfig.catalogUrl = previousUrl
        }
    }

    private class LocalHttpServer(
        private val response: ByteArray? = null,
        private val holdOpenMillis: Long = 0,
        private val disconnectWithoutResponse: Boolean = false,
    ) : Closeable {
        private val server = ServerSocket(0)
        @Volatile var requestLine: String = ""
            private set
        @Volatile var requestHeaders: String = ""
            private set
        val url: String = "http://127.0.0.1:" + server.localPort + "/catalog.json?key=not-a-secret"

        private val worker = thread(isDaemon = true, name = "catalog-test-http-server") {
            try {
                server.accept().use { socket ->
                    socket.soTimeout = 1_000
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), UTF_8))
                    requestLine = reader.readLine().orEmpty()
                    val headers = StringBuilder()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        headers.append(line).append('\n')
                    }
                    requestHeaders = headers.toString()

                    when {
                        disconnectWithoutResponse -> Unit
                        holdOpenMillis > 0 -> Thread.sleep(holdOpenMillis)
                        response != null -> {
                            socket.getOutputStream().apply {
                                write(response)
                                flush()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Socket closure is expected in timeout/disconnect test cases.
            }
        }

        override fun close() {
            runCatching { server.close() }
            worker.join(1_000)
        }
    }

    companion object {
        private fun validCatalogJson(): String =
            """
            {
              "version": 1,
              "podcasts": [{
                "id": "remote-podcast",
                "title": "Remote podcast",
                "description": "Fetched over localhost HTTP",
                "coverUrl": "https://cdn.example/cover.jpg",
                "isAiGenerated": true,
                "episodeCount": 1,
                "category": "علم"
              }],
              "episodes": [{
                "id": "remote-episode",
                "podcastId": "remote-podcast",
                "title": "Remote episode",
                "audioUrl": "https://cdn.example/audio.mp3",
                "durationMs": 90000,
                "publishedAtEpochMs": 1700000000000
              }]
            }
            """.trimIndent()

        private fun httpResponse(statusCode: Int, body: String): ByteArray {
            val reason = when (statusCode) {
                200 -> "OK"
                404 -> "Not Found"
                503 -> "Service Unavailable"
                else -> "Test Response"
            }
            val payload = body.toByteArray(UTF_8)
            val headers = "HTTP/1.1 $statusCode $reason\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${payload.size}\r\n" +
                "Connection: close\r\n\r\n"
            return headers.toByteArray(UTF_8) + payload
        }
    }
}
