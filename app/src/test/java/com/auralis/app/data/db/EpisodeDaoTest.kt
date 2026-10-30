package com.auralis.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EpisodeDaoTest {

    private lateinit var context: Context
    private lateinit var db: AuralisDatabase
    private lateinit var dao: EpisodeDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AuralisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.episodeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: String,
        title: String = "title-$id",
        remoteId: String? = null,
        localUri: String? = null,
        lastPlayedAt: Long? = null,
    ) = EpisodeEntity(
        id = id,
        title = title,
        remoteId = remoteId,
        localUri = localUri,
        lastPlayedAt = lastPlayedAt,
    )

    @Test
    fun upsertThenGetByIdReturnsTheSameRow() = runBlocking {
        val e = entity("a", title = "عنوان", localUri = "content://x/1")
        dao.upsert(e)
        assertEquals(e, dao.getById("a"))
    }

    @Test
    fun upsertWithSameIdReplacesInsteadOfDuplicating() = runBlocking {
        dao.upsert(entity("a", title = "old"))
        dao.upsert(entity("a", title = "new"))
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("new", all.single().title)
    }

    @Test
    fun getByIdOnMissingRowIsNull() = runBlocking {
        assertNull(dao.getById("missing"))
    }

    @Test
    fun getByRemoteIdFindsCatalogEpisode() = runBlocking {
        dao.upsert(entity("a", remoteId = "remote-1"))
        dao.upsert(entity("b"))
        assertEquals("a", dao.getByRemoteId("remote-1")?.id)
        assertNull(dao.getByRemoteId("remote-404"))
    }

    @Test
    fun updatePositionStoresPositionAndTimestampOnly() = runBlocking {
        dao.upsert(entity("a", title = "keep", localUri = "content://x/1"))
        dao.updatePosition("a", positionMs = 42_000L, playedAt = 1_700_000_000_000L)

        val row = dao.getById("a")!!
        assertEquals(42_000L, row.lastPositionMs)
        assertEquals(1_700_000_000_000L, row.lastPlayedAt)
        assertEquals("keep", row.title)
        assertEquals("content://x/1", row.localUri)
    }

    @Test
    fun updatePositionOnMissingRowDoesNotThrowOrCreateARow() = runBlocking {
        dao.updatePosition("ghost", positionMs = 1L, playedAt = 2L)
        assertNull(dao.getById("ghost"))
        assertEquals(0, dao.observeAll().first().size)
    }

    @Test
    fun updateLocalUriSetsAndClearsTheSource() = runBlocking {
        dao.upsert(entity("a"))
        dao.updateLocalUri("a", "content://x/9")
        assertEquals("content://x/9", dao.getById("a")?.localUri)
        // A deleted/unreadable local file can be detached without losing the episode.
        dao.updateLocalUri("a", null)
        val row = dao.getById("a")
        assertNotNull(row)
        assertNull(row?.localUri)
    }

    @Test
    fun deleteRemovesOnlyTheTargetRow() = runBlocking {
        dao.upsert(entity("a"))
        dao.upsert(entity("b"))
        dao.delete("a")
        assertNull(dao.getById("a"))
        assertNotNull(dao.getById("b"))
    }

    @Test
    fun deleteOnMissingRowDoesNotThrow() = runBlocking {
        dao.delete("ghost")
        assertEquals(0, dao.observeAll().first().size)
    }

    @Test
    fun observeAllOrdersRecentlyPlayedFirstThenUnplayedByTitle() = runBlocking {
        dao.upsert(entity("a", title = "x", lastPlayedAt = 100L))
        dao.upsert(entity("b", title = "x", lastPlayedAt = 300L))
        dao.upsert(entity("c", title = "B"))
        dao.upsert(entity("d", title = "A"))
        assertEquals(listOf("b", "a", "d", "c"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun libraryAndPositionSurviveClosingAndReopeningTheDatabaseFile() = runBlocking {
        val name = "restart-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, AuralisDatabase::class.java, name)
                .allowMainThreadQueries()
                .build()
            first.episodeDao().upsert(entity("a", title = "persisted", localUri = "content://x/1"))
            first.episodeDao().updatePosition("a", positionMs = 77_000L, playedAt = 1_700_000_000_000L)
            first.close()

            // A new database object on the same file simulates killing and reopening the app.
            val second = Room.databaseBuilder(context, AuralisDatabase::class.java, name)
                .allowMainThreadQueries()
                .build()
            try {
                val restored = second.episodeDao().getById("a")
                assertNotNull(restored)
                assertEquals("persisted", restored?.title)
                assertEquals("content://x/1", restored?.localUri)
                assertEquals(77_000L, restored?.lastPositionMs)
                assertEquals(1_700_000_000_000L, restored?.lastPlayedAt)
                assertEquals(1, second.episodeDao().observeAll().first().size)
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
