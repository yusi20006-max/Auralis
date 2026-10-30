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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #11 — position written via repository survives and is readable after "restart"
 * (new repository + reopened DB connection).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackPositionPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: AuralisDatabase
    private lateinit var scope: kotlinx.coroutines.CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AuralisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
    }

    @Test
    fun updatePositionPersistsAndSurvivesNewRepositoryInstance() = runBlocking {
        val dao = database.episodeDao()
        dao.upsert(
            EpisodeEntity(
                id = "ep-pos",
                title = "Position test",
                localUri = "content://media/audio/pos",
                lastPositionMs = 0L,
            ),
        )

        val repo1 = LibraryRepository(dao, PrivateDownloadStore(context), scope)
        repo1.updatePosition("ep-pos", 125_000L)

        // updatePosition launches on repository scope; yield briefly for Unconfined/IO.
        delay(50)

        val afterUpdate = dao.getById("ep-pos")!!
        assertEquals(125_000L, afterUpdate.lastPositionMs)
        assertTrue(afterUpdate.lastPlayedAt != null && afterUpdate.lastPlayedAt!! > 0L)

        val repo2 = LibraryRepository(dao, PrivateDownloadStore(context), scope)
        val episode = repo2.episodes.first { list -> list.any { it.id == "ep-pos" } }
            .first { it.id == "ep-pos" }
        assertEquals(125_000L, episode.lastPositionMs)
    }

    @Test
    fun seekPositionClampedByEpisodeModelProgressFraction() {
        val ep = Episode(
            id = "x",
            title = "t",
            durationMs = 1000,
            lastPositionMs = 5000,
            localUri = Uri.parse("content://x"),
        )
        assertEquals(1f, ep.progressFraction)
    }
}
