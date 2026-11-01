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
import com.auralis.app.data.download.PrivateDownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
