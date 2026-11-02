package com.auralis.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.auralis.app.data.catalog.CatalogConfig
import com.auralis.app.data.catalog.CatalogRepository
import com.auralis.app.data.db.AuralisDatabase
import com.auralis.app.data.download.DownloadCancelledException
import com.auralis.app.data.download.DownloadIdentity
import com.auralis.app.data.download.InsufficientSpaceException
import com.auralis.app.data.download.DownloadStore
import com.auralis.app.data.download.PrivateDownloadStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Cross-layer regression coverage for Issue #30.
 *
 * These tests connect the real CatalogRepository, Room-backed LibraryRepository,
 * DownloadStore contract, local URI resolver and persisted playback position.
 * They do not claim to exercise Media3 decoding or a physical Android device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogLibraryPlaybackRegressionTest {

    private lateinit var context: Context
    private lateinit var database: AuralisDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var downloads: FileBackedDownloadStore
    private lateinit var library: LibraryRepository
    private lateinit var catalog: CatalogRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AuralisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        downloads = FileBackedDownloadStore(context)
        library = LibraryRepository(database.episodeDao(), downloads, scope)
        catalog = CatalogRepository()
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
        downloads.close()
    }

    @Test
    fun catalogBrowseAndPlayDoNotPersistMembershipButAddAndPositionSurviveRestart() = runBlocking {
        val podcast = requireNotNull(catalog.getPodcast("ai-philosophy"))
        val catalogEpisode = requireNotNull(catalog.getEpisode("ep-philo-1"))
        assertTrue(catalog.observePodcasts().first().any { it.id == podcast.id })
        assertTrue(catalog.observeEpisodes(podcast.id).first().any { it.id == catalogEpisode.id })

        // Resolving a stream item for Play must not insert a Room row or Library member.
        val streamEpisode = library.addOrGetStreamEpisode(catalogEpisode, podcast)
        assertEquals(CatalogConfig.SOURCE_ID, streamEpisode.sourceId)
        assertTrue(database.episodeDao().observeAll().first().isEmpty())
        assertTrue(library.libraryEpisodes.first().isEmpty())
        val streamUri = LocalUriAccess.resolvePlayableUri(context, streamEpisode)
        assertEquals(catalogEpisode.audioUrl, streamUri?.toString())

        val added = library.addToLibrary(catalogEpisode, podcast)
        assertTrue(added.isInLibrary)
        assertEquals(1, library.libraryEpisodes.first { rows -> rows.any { it.id == added.id } }.size)
        library.updatePosition(added.id, 37_500L)
        val persisted = database.episodeDao().observeAll().first { rows ->
            rows.any { it.id == added.id && it.lastPositionMs == 37_500L }
        }.single { it.id == added.id }
        assertEquals(37_500L, persisted.lastPositionMs)

        // Re-open a file-backed Room database to model process/database recreation.
        val databaseName = "issue30-library-restart-${System.nanoTime()}.db"
        val firstDatabase = Room.databaseBuilder(context, AuralisDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val firstRepository = LibraryRepository(firstDatabase.episodeDao(), downloads, firstScope)
            val durable = firstRepository.addToLibrary(catalogEpisode, podcast)
            firstRepository.updatePosition(durable.id, 52_000L)
            firstDatabase.episodeDao().observeAll().first { rows ->
                rows.any { it.id == durable.id && it.lastPositionMs == 52_000L }
            }
            firstDatabase.close()

            val reopened = Room.databaseBuilder(context, AuralisDatabase::class.java, databaseName)
                .allowMainThreadQueries()
                .build()
            try {
                val restoredRepository = LibraryRepository(reopened.episodeDao(), downloads, firstScope)
                val restored = restoredRepository.libraryEpisodes.first { rows ->
                    rows.any { it.id == durable.id }
                }.single { it.id == durable.id }
                assertTrue(restored.isInLibrary)
                assertEquals(52_000L, restored.lastPositionMs)
            } finally {
                reopened.close()
            }
        } finally {
            runCatching { firstDatabase.close() }
            firstScope.cancel()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun catalogDownloadFailureRetryOfflineResolutionAndExplicitDeleteRemainIndependent() = runBlocking {
        val podcast = requireNotNull(catalog.getPodcast("ai-philosophy"))
        val catalogEpisode = requireNotNull(catalog.getEpisode("ep-philo-1"))
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, catalogEpisode.id)
        downloads.failNext(identity)

        assertNull(library.downloadEpisodeOffline(identity, catalogEpisode, podcast))
        assertTrue(library.downloadErrors.value.containsKey(identity))
        assertTrue(database.episodeDao().observeAll().first().isEmpty())

        val downloaded = requireNotNull(library.downloadEpisodeOffline(identity, catalogEpisode, podcast))
        assertFalse(downloaded.isInLibrary)
        assertTrue(library.isDownloaded(identity))
        assertTrue(library.libraryEpisodes.first().isEmpty())
        assertNotNull(downloaded.localUri)

        // A stale row URI must not prevent the resolver from selecting a valid private copy.
        val stale = Uri.fromFile(File(context.cacheDir, "missing-issue30-${System.nanoTime()}.mp3"))
        val privateUri = requireNotNull(library.privateDownloadUri(downloaded))
        assertEquals(
            privateUri,
            LocalUriAccess.resolvePlayableUri(
                context,
                downloaded.copy(localUri = stale),
                privateDownloadUri = privateUri,
            ),
        )

        val member = library.addToLibrary(catalogEpisode, podcast)
        assertEquals(downloaded.id, member.id)
        assertTrue(member.isInLibrary)
        library.deleteDownload(member.id)
        val afterDelete = database.episodeDao().observeAll().first { rows ->
            rows.any { it.id == member.id && it.localUri == null }
        }.single { it.id == member.id }
        assertTrue("deleting the offline copy must preserve explicit Library membership", afterDelete.isInLibrary)
        assertFalse(library.isDownloaded(identity))
        assertEquals(
            catalogEpisode.audioUrl,
            LocalUriAccess.resolvePlayableUri(context, member.copy(localUri = null))?.toString(),
        )
    }

    @Test
    fun cancellationLeavesNoPartialEpisodeAndTheSameCatalogItemCanBeRetried() = runBlocking {
        val podcast = requireNotNull(catalog.getPodcast("ai-philosophy"))
        val catalogEpisode = requireNotNull(catalog.getEpisode("ep-philo-1"))
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, catalogEpisode.id)
        downloads.blockNextDownload()

        val pending = async(Dispatchers.IO) {
            library.downloadEpisodeOffline(identity, catalogEpisode, podcast)
        }
        assertTrue(downloads.started.await(5, TimeUnit.SECONDS))
        library.cancelDownload(identity)
        downloads.release.countDown()

        assertNull(pending.await())
        assertFalse(library.isDownloaded(identity))
        assertTrue(database.episodeDao().observeAll().first().isEmpty())
        assertFalse(library.downloadingIdentities.value.contains(identity))

        val retried = requireNotNull(library.downloadEpisodeOffline(identity, catalogEpisode, podcast))
        assertTrue(library.isDownloaded(identity))
        assertEquals(identity.remoteId, retried.remoteId)
    }

    @Test
    fun insufficientStorageIsReportedWithoutPersistingDownloadRecord() = runBlocking {
        val podcast = requireNotNull(catalog.getPodcast("ai-philosophy"))
        val episode = catalog.getEpisode("ep-philo-1")!!.copy(
            id = "issue30-no-space-${System.nanoTime()}",
        )
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, episode.id)
        downloads.failForInsufficientSpace(identity)

        assertNull(library.downloadEpisodeOffline(identity, episode, podcast))

        assertEquals("فضای ذخیره‌سازی کافی نیست.", library.downloadErrors.value[identity])
        assertFalse(library.isDownloaded(identity))
        assertTrue(database.episodeDao().observeAll().first().none { it.remoteId == episode.id })
    }

    @Test
    fun sameRemoteIdAcrossSourcesAndLocalImportRemainIndependent() = runBlocking {
        val podcast = requireNotNull(catalog.getPodcast("ai-philosophy"))
        val catalogEpisode = requireNotNull(catalog.getEpisode("ep-philo-1"))
        val catalogIdentity = DownloadIdentity("catalog:auralis", catalogEpisode.id)
        val otherIdentity = DownloadIdentity("rss:feed-42", catalogEpisode.id)

        val catalogDownload = requireNotNull(
            library.downloadEpisodeOffline(catalogIdentity, catalogEpisode, podcast),
        )
        val otherDownload = requireNotNull(
            library.downloadEpisodeOffline(otherIdentity, catalogEpisode, podcast),
        )
        assertNotEquals(catalogDownload.id, otherDownload.id)
        assertNotEquals(catalogDownload.localUri, otherDownload.localUri)

        val rows = database.episodeDao().observeAll().first { it.size == 2 }
        assertEquals(1, rows.count { it.sourceId == catalogIdentity.sourceId && it.remoteId == catalogEpisode.id })
        assertEquals(1, rows.count { it.sourceId == otherIdentity.sourceId && it.remoteId == catalogEpisode.id })

        val localFile = File.createTempFile("auralis-issue30-local-", ".mp3", context.cacheDir)
        localFile.writeBytes(ByteArray(256) { 7 })
        try {
            val local = library.addLocalEpisode(Uri.fromFile(localFile), "Imported local media")
            assertNull(local.sourceId)
            assertNull(local.remoteId)
            assertTrue(local.isInLibrary)
            val allRows = database.episodeDao().observeAll().first { it.size == 3 }
            assertEquals(1, allRows.count { it.localUri == Uri.fromFile(localFile).toString() })
            assertTrue(allRows.any { it.id == local.id && it.sourceId == null && it.isInLibrary })
        } finally {
            localFile.delete()
        }
    }


    @Test
    fun interruptedHttpDownloadRemovesPartialFileAndDoesNotPersistEpisode() = runBlocking {
        val podcast = requireNotNull(catalog.getPodcast("ai-philosophy"))
        PartialAudioServer().use { server ->
            val store = PrivateDownloadStore(context)
            val repository = LibraryRepository(database.episodeDao(), store, scope)
            val episode = catalog.getEpisode("ep-philo-1")!!.copy(
                id = "issue30-truncated-${System.nanoTime()}",
                audioUrl = server.url,
            )
            val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, episode.id)

            assertNull(repository.downloadEpisodeOffline(identity, episode, podcast))
            assertTrue("the production download store must reach the local HTTP server", server.bodySent.await(5, TimeUnit.SECONDS))

            assertTrue(repository.downloadErrors.value.containsKey(identity))
            assertFalse(store.fileForIdentity(identity.sourceId, identity.remoteId).exists())
            assertFalse(store.partialFileForIdentity(identity.sourceId, identity.remoteId).exists())
            assertTrue(database.episodeDao().observeAll().first().none { it.remoteId == episode.id })
        }
    }

    private class PartialAudioServer : Closeable {
        private val server = ServerSocket(0)
        val bodySent = CountDownLatch(1)
        val url = "http://127.0.0.1:${server.localPort}/audio.mp3"
        private val worker = thread(isDaemon = true, name = "issue30-partial-audio-server") {
            try {
                server.accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    val output = socket.getOutputStream()
                    output.write(
                        ("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: audio/mpeg\r\n" +
                            "Content-Length: 2048\r\n" +
                            "Connection: close\r\n\r\n").toByteArray(),
                    )
                    output.write(ByteArray(128) { 4 })
                    output.flush()
                    bodySent.countDown()
                    socket.shutdownOutput()
                }
            } catch (_: Exception) {
                // The client may disconnect immediately after the truncated response.
            }
        }

        override fun close() {
            runCatching { server.close() }
            runCatching { worker.join(1_000) }
        }
    }

    private class FileBackedDownloadStore(context: Context) : DownloadStore {
        private val root = File(context.cacheDir, "issue30-downloads-${System.nanoTime()}")
        private val files = ConcurrentHashMap<DownloadIdentity, File>()
        private val cancelled = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        private val failures = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        private val insufficientSpace = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        @Volatile private var blockNext = false
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        init {
            root.mkdirs()
        }

        fun failNext(identity: DownloadIdentity) {
            failures.add(identity)
        }

        fun failForInsufficientSpace(identity: DownloadIdentity) {
            insufficientSpace.add(identity)
        }

        fun blockNextDownload() {
            blockNext = true
        }

        override fun hasLocalCopy(identity: DownloadIdentity): Boolean =
            files[identity]?.let { it.isFile && it.length() > 64L } == true

        override fun uriForIdentity(identity: DownloadIdentity): Uri? =
            files[identity]?.takeIf { it.isFile }?.let(Uri::fromFile)

        override fun cancel(identity: DownloadIdentity) {
            cancelled.add(identity)
        }

        override fun download(
            identity: DownloadIdentity,
            url: String,
            onProgress: ((Float) -> Unit)?,
        ): Uri {
            if (blockNext) {
                blockNext = false
                started.countDown()
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw IOException("Timed out waiting for cancellation test")
                }
            }
            if (cancelled.remove(identity)) throw DownloadCancelledException(identity.remoteId)
            if (insufficientSpace.remove(identity)) throw InsufficientSpaceException(0L)
            if (failures.remove(identity)) throw IOException("Simulated transient download failure")

            val file = File(root, identity.hashCode().toString() + ".mp3")
            file.writeBytes(ByteArray(256) { 3 })
            files[identity] = file
            onProgress?.invoke(1f)
            return Uri.fromFile(file)
        }

        override fun deleteExact(identity: DownloadIdentity): Boolean {
            val file = files.remove(identity) ?: return false
            return !file.exists() || file.delete()
        }

        fun close() {
            root.deleteRecursively()
        }
    }
}
