package com.auralis.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

        // The repository StateFlow is intentionally not collected in this test.
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
}
