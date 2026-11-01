package com.auralis.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.auralis.app.data.catalog.CatalogConfig
import com.auralis.app.data.catalog.CatalogEpisode
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.db.AuralisDatabase
import com.auralis.app.data.db.EpisodeEntity
import com.auralis.app.data.download.DownloadCancelledException
import com.auralis.app.data.download.DownloadIdentity
import com.auralis.app.data.download.DownloadStore
import com.auralis.app.data.download.PrivateDownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AuralisDatabase
    private lateinit var repositoryScope: kotlinx.coroutines.CoroutineScope
    private lateinit var repository: LibraryRepository

    private val samplePodcast = CatalogPodcast(
        id = "ai-philosophy",
        title = "Philosophy",
        description = "test",
    )

    private val sampleEpisode = CatalogEpisode(
        id = "ep-philo-1",
        podcastId = "ai-philosophy",
        title = "Can machines understand?",
        description = "desc",
        audioUrl = "https://example.com/a.mp3",
        durationMs = 60_000L,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AuralisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repositoryScope = kotlinx.coroutines.CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined,
        )
        repository = LibraryRepository(
            database.episodeDao(),
            PrivateDownloadStore(context),
            repositoryScope,
        )
    }

    private fun repositoryDownloadStoreFile(sourceId: String, remoteId: String) =
        PrivateDownloadStore(context).fileForIdentity(sourceId, remoteId)

    @After
    fun tearDown() {
        repositoryScope.cancel()
        database.close()
    }

    @Test
    fun addLocalEpisodeReturnsExistingDatabaseRowBeforeFlowEmits() = runBlocking {
        val uri = Uri.parse("content://media/audio/existing")
        val existing = EpisodeEntity(
            id = "persisted-id",
            title = "Already saved",
            localUri = uri.toString(),
        )
        database.episodeDao().upsert(existing)

        val returned = repository.addLocalEpisode(uri, "New title")

        assertEquals("persisted-id", returned.id)
        assertEquals("Already saved", returned.title)
        assertEquals(1, database.episodeDao().observeAll().first().size)
    }

    @Test
    fun addLocalEpisodePersistsDifferentUrisAsSeparateRows() = runBlocking {
        val first = repository.addLocalEpisode(
            Uri.parse("content://media/audio/one"),
            "One",
        )
        val second = repository.addLocalEpisode(
            Uri.parse("content://media/audio/two"),
            "Two",
        )

        assertNotEquals(first.id, second.id)
        val rows = database.episodeDao().observeAll().first()
        assertEquals(2, rows.size)
    }

    @Test
    fun concurrentAddsForSameUriReturnOnePersistedEpisode() = runBlocking {
        val uri = Uri.parse("content://media/audio/concurrent")
        val results = (1..12).map { index ->
            async(Dispatchers.IO) {
                repository.addLocalEpisode(uri, "Title $index")
            }
        }.map { it.await() }

        assertEquals(1, results.map { it.id }.toSet().size)
        val rows = database.episodeDao().observeAll().first()
        assertEquals(1, rows.size)
        assertTrue(results.all { it.localUri == uri })
        assertEquals(rows.single().id, results.first().id)
    }

    @Test
    fun removingLibraryMembershipPreservesDownloadedFile() = runBlocking {
        val remoteId = "preserve-download-${System.nanoTime()}"
        val sourceId = CatalogConfig.SOURCE_ID
        val file = repositoryDownloadStoreFile(sourceId, remoteId)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(128) { 5 })
        val episodeId = "downloaded-row-${System.nanoTime()}"
        database.episodeDao().upsert(
            EpisodeEntity(
                id = episodeId,
                title = "Downloaded episode",
                remoteId = remoteId,
                sourceId = sourceId,
                localUri = Uri.parse("content://test/downloaded").toString(),
            ),
        )

        repository.deleteEpisode(episodeId)
        database.episodeDao().observeAll().first { rows -> rows.none { it.id == episodeId } }

        assertTrue("removing Library membership must preserve the offline file", file.isFile)
        assertTrue(file.length() > 64L)
        file.delete()
        Unit
    }

    @Test
    fun explicitDeleteDownloadRemovesExactFileButKeepsLibraryMembership() = runBlocking {
        val remoteId = "explicit-delete-${System.nanoTime()}"
        val sourceId = CatalogConfig.SOURCE_ID
        val file = repositoryDownloadStoreFile(sourceId, remoteId)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(128) { 6 })
        val episodeId = "explicit-download-row-${System.nanoTime()}"
        database.episodeDao().upsert(
            EpisodeEntity(
                id = episodeId,
                title = "Keep library row",
                remoteId = remoteId,
                sourceId = sourceId,
                localUri = Uri.parse("content://test/private-download").toString(),
            ),
        )

        repository.deleteDownload(episodeId)
        val updated = database.episodeDao().observeAll().first { rows ->
            rows.any { it.id == episodeId && it.localUri == null }
        }

        assertEquals(1, updated.size)
        assertTrue("explicit delete should remove the exact private file", !file.exists())
        assertNotNull(database.episodeDao().getById(episodeId))
    }

    @Test
    fun explicitDeleteDownloadPreservesAmbiguousLegacyFileAndUri() = runBlocking {
        val remoteId = "legacy-explicit-delete-${System.nanoTime()}"
        val legacyFile = PrivateDownloadStore(context).fileForRemoteId(remoteId)
        legacyFile.parentFile?.mkdirs()
        legacyFile.writeBytes(ByteArray(128) { 8 })
        val episodeId = "legacy-download-row-${System.nanoTime()}"
        val legacyUri = Uri.fromFile(legacyFile).toString()
        database.episodeDao().upsert(
            EpisodeEntity(
                id = episodeId,
                title = "Ambiguous legacy download",
                remoteId = remoteId,
                sourceId = null,
                localUri = legacyUri,
            ),
        )

        repository.deleteDownload(episodeId)
        val row = database.episodeDao().getById(episodeId)

        assertTrue("ambiguous legacy file must be preserved", legacyFile.isFile)
        assertEquals("legacy URI must remain available", legacyUri, row?.localUri)
        assertNotNull(row)
        legacyFile.delete()
        Unit
    }

    @Test
    fun removeFromLibraryPreservesExactPrivateDownloadAndDownloadedStatus() = runBlocking {
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, sampleEpisode.id)
        val file = repositoryDownloadStoreFile(identity.sourceId!!, identity.remoteId)
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(256) { 11 })
        val added = repository.addToLibrary(sampleEpisode, samplePodcast)
        assertNotNull(added.localUri)

        assertTrue(repository.removeFromLibrary(identity))

        assertNull(database.episodeDao().getBySourceAndRemoteId(identity.sourceId, identity.remoteId))
        assertTrue("removing Library membership must not delete the private file", file.isFile)
        assertTrue(repository.isDownloaded(identity))

        val reAdded = repository.addToLibrary(sampleEpisode, samplePodcast)
        assertNotNull(reAdded.localUri)
        assertNotNull(database.episodeDao().getBySourceAndRemoteId(identity.sourceId, identity.remoteId))
        assertTrue("re-adding membership must reuse the existing private file", file.isFile)
        file.delete()
        Unit
    }

    @Test
    fun concurrentAddToLibraryRequestsCreateExactlyOneDurableMembership() = runBlocking {
        val results = (1..12).map {
            async(Dispatchers.IO) { repository.addToLibrary(sampleEpisode, samplePodcast) }
        }.map { it.await() }

        assertEquals(1, results.map { it.id }.toSet().size)
        val rows = database.episodeDao().observeAll().first()
        assertEquals(1, rows.count {
            it.sourceId == CatalogConfig.SOURCE_ID && it.remoteId == sampleEpisode.id
        })
    }

    @Test
    fun downloadCreatesItsOwnLibraryRecordWithoutPriorAddAction() = runBlocking {
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, sampleEpisode.id)
        val store = RecordingDownloadStore()
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)

        val downloaded = testRepository.downloadEpisodeOffline(identity, sampleEpisode, samplePodcast)

        assertNotNull(downloaded)
        assertTrue(store.completed.contains(identity))
        val row = database.episodeDao().getBySourceAndRemoteId(identity.sourceId!!, identity.remoteId)
        assertNotNull(row)
        assertNotNull(row?.localUri)
        assertEquals(1, database.episodeDao().observeAll().first().size)
    }

    @Test
    fun explicitLibraryMembershipSurvivesDatabaseRestart() = runBlocking {
        val databaseName = "catalog-library-restart-" + System.nanoTime() + ".db"
        val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, sampleEpisode.id)
        val persistentScope = kotlinx.coroutines.CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined,
        )
        val firstDatabase = Room.databaseBuilder(
            context,
            AuralisDatabase::class.java,
            databaseName,
        ).allowMainThreadQueries().build()

        try {
            val persistentRepository = LibraryRepository(
                firstDatabase.episodeDao(),
                PrivateDownloadStore(context),
                persistentScope,
            )
            val added = persistentRepository.addToLibrary(sampleEpisode, samplePodcast)
            assertNotNull(firstDatabase.episodeDao().getById(added.id))
            firstDatabase.close()

            val reopenedDatabase = Room.databaseBuilder(
                context,
                AuralisDatabase::class.java,
                databaseName,
            ).allowMainThreadQueries().build()
            try {
                val restored = reopenedDatabase.episodeDao().getBySourceAndRemoteId(
                    identity.sourceId!!,
                    identity.remoteId,
                )
                assertNotNull(restored)
                assertEquals(added.id, restored?.id)
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            runCatching { firstDatabase.close() }
            persistentScope.cancel()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun streamResolveDoesNotCreatePermanentLibraryMembership() = runBlocking {
        val resolved = repository.addOrGetStreamEpisode(sampleEpisode, samplePodcast)

        assertEquals(CatalogConfig.SOURCE_ID, resolved.sourceId)
        assertEquals(sampleEpisode.id, resolved.remoteId)
        assertEquals(0, database.episodeDao().observeAll().first().size)
    }

    @Test
    fun addToLibraryCreatesDurableMembershipWithSourceId() = runBlocking {
        val added = repository.addToLibrary(sampleEpisode, samplePodcast)

        assertEquals(CatalogConfig.SOURCE_ID, added.sourceId)
        assertEquals(sampleEpisode.id, added.remoteId)

        val rows = database.episodeDao().observeAll().first()
        assertEquals(1, rows.size)
        assertEquals(CatalogConfig.SOURCE_ID, rows.single().sourceId)
        assertEquals(sampleEpisode.id, rows.single().remoteId)
    }

    @Test
    fun addToLibraryIsIdempotentForSameCompositeIdentity() = runBlocking {
        val first = repository.addToLibrary(sampleEpisode, samplePodcast)
        val second = repository.addToLibrary(sampleEpisode, samplePodcast)

        assertEquals(first.id, second.id)
        assertEquals(1, database.episodeDao().observeAll().first().size)
    }

    @Test
    fun streamResolveReturnsExistingLibraryRowAfterAddToLibrary() = runBlocking {
        val added = repository.addToLibrary(sampleEpisode, samplePodcast)
        repository.episodes.first { list -> list.any { it.id == added.id } }

        val resolved = repository.addOrGetStreamEpisode(sampleEpisode, samplePodcast)
        assertEquals(added.id, resolved.id)
        assertEquals(CatalogConfig.SOURCE_ID, resolved.sourceId)
    }

    @Test
    fun legacyNullSourceRowIsNotClaimedByCatalogCompositeLookup() = runBlocking {
        database.episodeDao().upsert(
            EpisodeEntity(
                id = "legacy-row",
                title = "Legacy",
                remoteId = sampleEpisode.id,
                sourceId = null,
            ),
        )
        repository.episodes.first { it.any { row -> row.id == "legacy-row" } }

        val resolved = repository.addOrGetStreamEpisode(sampleEpisode, samplePodcast)
        assertNotEquals("legacy-row", resolved.id)
        assertEquals(CatalogConfig.SOURCE_ID, resolved.sourceId)

        val legacy = database.episodeDao().getById("legacy-row")
        assertNotNull(legacy)
        assertNull(legacy!!.sourceId)
    }
    @Test
    fun sameRemoteIdFromDifferentSourcesDownloadsConcurrentlyWithIndependentProgress() = runBlocking {
        val store = RecordingDownloadStore(started = CountDownLatch(2), release = CountDownLatch(1))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val first = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val second = DownloadIdentity("rss:feed-42", sampleEpisode.id)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast) }
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(second, sampleEpisode, samplePodcast) }

        assertTrue("both source identities should enter download concurrently", store.started!!.await(5, TimeUnit.SECONDS))
        val active = testRepository.downloadingIdentities.first { it.contains(first) && it.contains(second) }
        assertEquals(setOf(first, second), active)
        val progress = testRepository.downloadProgress.first { it[first] == 0.25f && it[second] == 0.75f }
        assertEquals(0.25f, progress[first]!!, 0.001f)
        assertEquals(0.75f, progress[second]!!, 0.001f)

        store.release!!.countDown()
        val firstEpisode = firstTask.await()
        val secondEpisode = secondTask.await()
        assertEquals(first.sourceId, firstEpisode?.sourceId)
        assertEquals(second.sourceId, secondEpisode?.sourceId)
        assertEquals(2, store.totalCalls.get())
        assertFalse(testRepository.downloadingIdentities.value.contains(first))
        assertFalse(testRepository.downloadingIdentities.value.contains(second))
        assertFalse(testRepository.downloadProgress.value.containsKey(first))
        assertFalse(testRepository.downloadProgress.value.containsKey(second))
    }

    @Test
    fun cancellingOneSourceDoesNotCancelAnotherWithSameRemoteId() = runBlocking {
        val store = RecordingDownloadStore(started = CountDownLatch(2), release = CountDownLatch(1))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val first = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val second = DownloadIdentity("rss:feed-42", sampleEpisode.id)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast) }
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(second, sampleEpisode, samplePodcast) }

        assertTrue(store.started!!.await(5, TimeUnit.SECONDS))
        testRepository.cancelDownload(first)
        store.release!!.countDown()
        assertNull(firstTask.await())
        assertEquals(second.sourceId, secondTask.await()?.sourceId)
        assertEquals(setOf(first), store.cancelled.toSet())
        assertTrue(store.completed.contains(second))
        assertFalse(store.completed.contains(first))
    }

    @Test
    fun retryErrorAndProgressRemainScopedToTheirSourceIdentity() = runBlocking {
        val first = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val second = DownloadIdentity("rss:feed-42", sampleEpisode.id)
        val store = RecordingDownloadStore(failNext = setOf(first))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast) }
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(second, sampleEpisode, samplePodcast) }

        assertNull(firstTask.await())
        assertEquals(second.sourceId, secondTask.await()?.sourceId)
        assertTrue(testRepository.downloadErrors.value.containsKey(first))
        assertFalse(testRepository.downloadErrors.value.containsKey(second))
        val retry = testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast)
        assertEquals(first.sourceId, retry?.sourceId)
        assertFalse(testRepository.downloadErrors.value.containsKey(first))
        assertEquals(2, store.callsFor(first))
        assertEquals(1, store.callsFor(second))
    }

    @Test
    fun concurrentRequestsForSameIdentityShareOneDownloadAndOneLibraryRow() = runBlocking {
        val identity = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val store = RecordingDownloadStore(started = CountDownLatch(1), release = CountDownLatch(1))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(identity, sampleEpisode, samplePodcast) }
        assertTrue(store.started!!.await(5, TimeUnit.SECONDS))
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(identity, sampleEpisode, samplePodcast) }
        delay(100)
        assertEquals("the second same-identity request must wait for the first", 1, store.callsFor(identity))
        store.release!!.countDown()
        val firstEpisode = firstTask.await()
        val secondEpisode = secondTask.await()
        assertNotNull(firstEpisode)
        assertEquals(firstEpisode?.id, secondEpisode?.id)
        assertEquals(1, store.callsFor(identity))
        assertEquals(1, database.episodeDao().observeAll().first().count {
            it.sourceId == identity.sourceId && it.remoteId == identity.remoteId
        })
    }

    private class RecordingDownloadStore(
        val started: CountDownLatch? = null,
        val release: CountDownLatch? = null,
        failNext: Set<DownloadIdentity> = emptySet(),
    ) : DownloadStore {
        val completed = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        val cancelled = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        private val failures = ConcurrentHashMap.newKeySet<DownloadIdentity>().apply { addAll(failNext) }
        private val calls = ConcurrentHashMap<DownloadIdentity, AtomicInteger>()
        val totalCalls = AtomicInteger()
        fun callsFor(identity: DownloadIdentity): Int = calls[identity]?.get() ?: 0

        override fun hasLocalCopy(identity: DownloadIdentity): Boolean = completed.contains(identity)
        override fun uriForIdentity(identity: DownloadIdentity): Uri? =
            if (completed.contains(identity)) Uri.parse("content://test/" + identity.sourceId + "/" + identity.remoteId) else null
        override fun cancel(identity: DownloadIdentity) { cancelled.add(identity) }

        override fun download(identity: DownloadIdentity, url: String, onProgress: ((Float) -> Unit)?): Uri {
            totalCalls.incrementAndGet()
            calls.computeIfAbsent(identity) { AtomicInteger() }.incrementAndGet()
            onProgress?.invoke(if (identity.sourceId == "catalog:auralis") 0.25f else 0.75f)
            started?.countDown()
            if (release != null && !release.await(5, TimeUnit.SECONDS)) throw IOException("Timed out waiting for test release")
            if (cancelled.contains(identity)) throw DownloadCancelledException(identity.remoteId)
            if (failures.remove(identity)) throw IOException("Simulated source-scoped failure")
            completed.add(identity)
            onProgress?.invoke(1f)
            return uriForIdentity(identity)!!
        }
        override fun deleteExact(identity: DownloadIdentity): Boolean = completed.remove(identity)
    }

    @Test
    fun sourceLessLegacyIdentityIsNotDownloadedOrAssignedToCatalogSource() = runBlocking {
        val legacy = DownloadIdentity(null, sampleEpisode.id)
        val store = RecordingDownloadStore()
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)

        val result = testRepository.downloadEpisodeOffline(legacy, sampleEpisode, samplePodcast)

        assertNull(result)
        assertEquals(0, store.totalCalls.get())
        assertTrue(testRepository.downloadErrors.value.containsKey(legacy))
        assertTrue(testRepository.downloadingIdentities.value.isEmpty())
    }


    @Test
    fun streamResolveDoesNotCreatePermanentLibraryMembership() = runBlocking {
        val resolved = repository.addOrGetStreamEpisode(sampleEpisode, samplePodcast)

        assertEquals(CatalogConfig.SOURCE_ID, resolved.sourceId)
        assertEquals(sampleEpisode.id, resolved.remoteId)
        assertEquals(0, database.episodeDao().observeAll().first().size)
    }

    @Test
    fun addToLibraryCreatesDurableMembershipWithSourceId() = runBlocking {
        val added = repository.addToLibrary(sampleEpisode, samplePodcast)

        assertEquals(CatalogConfig.SOURCE_ID, added.sourceId)
        assertEquals(sampleEpisode.id, added.remoteId)

        val rows = database.episodeDao().observeAll().first()
        assertEquals(1, rows.size)
        assertEquals(CatalogConfig.SOURCE_ID, rows.single().sourceId)
        assertEquals(sampleEpisode.id, rows.single().remoteId)
    }

    @Test
    fun addToLibraryIsIdempotentForSameCompositeIdentity() = runBlocking {
        val first = repository.addToLibrary(sampleEpisode, samplePodcast)
        val second = repository.addToLibrary(sampleEpisode, samplePodcast)

        assertEquals(first.id, second.id)
        assertEquals(1, database.episodeDao().observeAll().first().size)
    }

    @Test
    fun streamResolveReturnsExistingLibraryRowAfterAddToLibrary() = runBlocking {
        val added = repository.addToLibrary(sampleEpisode, samplePodcast)
        repository.episodes.first { list -> list.any { it.id == added.id } }

        val resolved = repository.addOrGetStreamEpisode(sampleEpisode, samplePodcast)
        assertEquals(added.id, resolved.id)
        assertEquals(CatalogConfig.SOURCE_ID, resolved.sourceId)
    }

    @Test
    fun legacyNullSourceRowIsNotClaimedByCatalogCompositeLookup() = runBlocking {
        database.episodeDao().upsert(
            EpisodeEntity(
                id = "legacy-row",
                title = "Legacy",
                remoteId = sampleEpisode.id,
                sourceId = null,
            ),
        )
        repository.episodes.first { it.any { row -> row.id == "legacy-row" } }

        val resolved = repository.addOrGetStreamEpisode(sampleEpisode, samplePodcast)
        assertNotEquals("legacy-row", resolved.id)
        assertEquals(CatalogConfig.SOURCE_ID, resolved.sourceId)

        val legacy = database.episodeDao().getById("legacy-row")
        assertNotNull(legacy)
        assertNull(legacy!!.sourceId)
    }
    @Test
    fun sameRemoteIdFromDifferentSourcesDownloadsConcurrentlyWithIndependentProgress() = runBlocking {
        val store = RecordingDownloadStore(started = CountDownLatch(2), release = CountDownLatch(1))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val first = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val second = DownloadIdentity("rss:feed-42", sampleEpisode.id)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast) }
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(second, sampleEpisode, samplePodcast) }

        assertTrue("both source identities should enter download concurrently", store.started!!.await(5, TimeUnit.SECONDS))
        val active = testRepository.downloadingIdentities.first { it.contains(first) && it.contains(second) }
        assertEquals(setOf(first, second), active)
        val progress = testRepository.downloadProgress.first { it[first] == 0.25f && it[second] == 0.75f }
        assertEquals(0.25f, progress[first]!!, 0.001f)
        assertEquals(0.75f, progress[second]!!, 0.001f)

        store.release!!.countDown()
        val firstEpisode = firstTask.await()
        val secondEpisode = secondTask.await()
        assertEquals(first.sourceId, firstEpisode?.sourceId)
        assertEquals(second.sourceId, secondEpisode?.sourceId)
        assertEquals(2, store.totalCalls.get())
        assertFalse(testRepository.downloadingIdentities.value.contains(first))
        assertFalse(testRepository.downloadingIdentities.value.contains(second))
        assertFalse(testRepository.downloadProgress.value.containsKey(first))
        assertFalse(testRepository.downloadProgress.value.containsKey(second))
    }

    @Test
    fun cancellingOneSourceDoesNotCancelAnotherWithSameRemoteId() = runBlocking {
        val store = RecordingDownloadStore(started = CountDownLatch(2), release = CountDownLatch(1))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val first = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val second = DownloadIdentity("rss:feed-42", sampleEpisode.id)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast) }
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(second, sampleEpisode, samplePodcast) }

        assertTrue(store.started!!.await(5, TimeUnit.SECONDS))
        testRepository.cancelDownload(first)
        store.release!!.countDown()
        assertNull(firstTask.await())
        assertEquals(second.sourceId, secondTask.await()?.sourceId)
        assertEquals(setOf(first), store.cancelled.toSet())
        assertTrue(store.completed.contains(second))
        assertFalse(store.completed.contains(first))
    }

    @Test
    fun retryErrorAndProgressRemainScopedToTheirSourceIdentity() = runBlocking {
        val first = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val second = DownloadIdentity("rss:feed-42", sampleEpisode.id)
        val store = RecordingDownloadStore(failNext = setOf(first))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast) }
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(second, sampleEpisode, samplePodcast) }

        assertNull(firstTask.await())
        assertEquals(second.sourceId, secondTask.await()?.sourceId)
        assertTrue(testRepository.downloadErrors.value.containsKey(first))
        assertFalse(testRepository.downloadErrors.value.containsKey(second))
        val retry = testRepository.downloadEpisodeOffline(first, sampleEpisode, samplePodcast)
        assertEquals(first.sourceId, retry?.sourceId)
        assertFalse(testRepository.downloadErrors.value.containsKey(first))
        assertEquals(2, store.callsFor(first))
        assertEquals(1, store.callsFor(second))
    }

    @Test
    fun concurrentRequestsForSameIdentityShareOneDownloadAndOneLibraryRow() = runBlocking {
        val identity = DownloadIdentity("catalog:auralis", sampleEpisode.id)
        val store = RecordingDownloadStore(started = CountDownLatch(1), release = CountDownLatch(1))
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)
        val firstTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(identity, sampleEpisode, samplePodcast) }
        assertTrue(store.started!!.await(5, TimeUnit.SECONDS))
        val secondTask = async(Dispatchers.IO) { testRepository.downloadEpisodeOffline(identity, sampleEpisode, samplePodcast) }
        delay(100)
        assertEquals("the second same-identity request must wait for the first", 1, store.callsFor(identity))
        store.release!!.countDown()
        val firstEpisode = firstTask.await()
        val secondEpisode = secondTask.await()
        assertNotNull(firstEpisode)
        assertEquals(firstEpisode?.id, secondEpisode?.id)
        assertEquals(1, store.callsFor(identity))
        assertEquals(1, database.episodeDao().observeAll().first().count {
            it.sourceId == identity.sourceId && it.remoteId == identity.remoteId
        })
    }

    private class RecordingDownloadStore(
        val started: CountDownLatch? = null,
        val release: CountDownLatch? = null,
        failNext: Set<DownloadIdentity> = emptySet(),
    ) : DownloadStore {
        val completed = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        val cancelled = ConcurrentHashMap.newKeySet<DownloadIdentity>()
        private val failures = ConcurrentHashMap.newKeySet<DownloadIdentity>().apply { addAll(failNext) }
        private val calls = ConcurrentHashMap<DownloadIdentity, AtomicInteger>()
        val totalCalls = AtomicInteger()
        fun callsFor(identity: DownloadIdentity): Int = calls[identity]?.get() ?: 0

        override fun hasLocalCopy(identity: DownloadIdentity): Boolean = completed.contains(identity)
        override fun uriForIdentity(identity: DownloadIdentity): Uri? =
            if (completed.contains(identity)) Uri.parse("content://test/" + identity.sourceId + "/" + identity.remoteId) else null
        override fun cancel(identity: DownloadIdentity) { cancelled.add(identity) }

        override fun download(identity: DownloadIdentity, url: String, onProgress: ((Float) -> Unit)?): Uri {
            totalCalls.incrementAndGet()
            calls.computeIfAbsent(identity) { AtomicInteger() }.incrementAndGet()
            onProgress?.invoke(if (identity.sourceId == "catalog:auralis") 0.25f else 0.75f)
            started?.countDown()
            if (release != null && !release.await(5, TimeUnit.SECONDS)) throw IOException("Timed out waiting for test release")
            if (cancelled.contains(identity)) throw DownloadCancelledException(identity.remoteId)
            if (failures.remove(identity)) throw IOException("Simulated source-scoped failure")
            completed.add(identity)
            onProgress?.invoke(1f)
            return uriForIdentity(identity)!!
        }
        override fun deleteExact(identity: DownloadIdentity): Boolean = completed.remove(identity)
    }

    @Test
    fun sourceLessLegacyIdentityIsNotDownloadedOrAssignedToCatalogSource() = runBlocking {
        val legacy = DownloadIdentity(null, sampleEpisode.id)
        val store = RecordingDownloadStore()
        val testRepository = LibraryRepository(database.episodeDao(), store, repositoryScope)

        val result = testRepository.downloadEpisodeOffline(legacy, sampleEpisode, samplePodcast)

        assertNull(result)
        assertEquals(0, store.totalCalls.get())
        assertTrue(testRepository.downloadErrors.value.containsKey(legacy))
        assertTrue(testRepository.downloadingIdentities.value.isEmpty())
    }

}
