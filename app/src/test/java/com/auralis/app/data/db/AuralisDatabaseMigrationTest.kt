package com.auralis.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AuralisDatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun migrationFromVersionOnePreservesLocalLegacyAndDownloadedEpisodes() = runBlocking {
        val databaseName = "migration-v1-to-v2.db"
        context.deleteDatabase(databaseName)
        try {
            createVersionOneDatabase(databaseName)

            val migrated = Room.databaseBuilder(
                context,
                AuralisDatabase::class.java,
                databaseName,
            )
                .addMigrations(AuralisDatabase.MIGRATION_1_2, AuralisDatabase.MIGRATION_2_3)
                .allowMainThreadQueries()
                .build()

            try {
                val dao = migrated.episodeDao()
                val local = dao.getById("local-1")!!
                val legacy = dao.getById("legacy-1")!!
                val downloaded = dao.getById("downloaded-1")!!

                assertEquals("content://media/local-1", local.localUri)
                assertNull(local.remoteId)
                assertNull(local.sourceId)
                assertEquals(true, local.isInLibrary)

                assertEquals("legacy-remote-1", legacy.remoteId)
                assertEquals("https://example.com/legacy.mp3", legacy.audioUrl)
                assertNull(legacy.sourceId)
                assertEquals(true, legacy.isInLibrary)

                assertEquals("download-remote-1", downloaded.remoteId)
                assertEquals("content://auralis/downloads/downloaded-1", downloaded.localUri)
                assertEquals(42_000L, downloaded.lastPositionMs)
                assertEquals(1_700_000_000_123L, downloaded.lastPlayedAt)
                assertEquals("Downloaded episode", downloaded.title)
                assertNull(downloaded.sourceId)
                assertEquals(true, downloaded.isInLibrary)
            } finally {
                migrated.close()
            }

            val reopened = Room.databaseBuilder(
                context,
                AuralisDatabase::class.java,
                databaseName,
            )
                .addMigrations(AuralisDatabase.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
            try {
                val restored = reopened.episodeDao().getById("downloaded-1")!!
                assertEquals("content://auralis/downloads/downloaded-1", restored.localUri)
                assertEquals(42_000L, restored.lastPositionMs)
                assertEquals(1_700_000_000_123L, restored.lastPlayedAt)
                assertNull(restored.sourceId)
                assertEquals(true, restored.isInLibrary)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionOneDatabase(databaseName: String) {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        val sqlite = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS episodes (" +
                    "id TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "podcastName TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "durationMs INTEGER NOT NULL, " +
                    "artworkUri TEXT, " +
                    "localUri TEXT, " +
                    "remoteId TEXT, " +
                    "audioUrl TEXT, " +
                    "isAiGenerated INTEGER NOT NULL, " +
                    "lastPositionMs INTEGER NOT NULL, " +
                    "lastPlayedAt INTEGER, " +
                    "PRIMARY KEY(id))"
            )
            sqlite.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (" +
                    "id INTEGER PRIMARY KEY, " +
                    "identity_hash TEXT)"
            )
            sqlite.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf("0009362eb2a194ed139e63c0750adb14"),
            )
            sqlite.version = 1

            insertEpisode(sqlite, id = "local-1", title = "Local episode", localUri = "content://media/local-1")
            insertEpisode(
                sqlite,
                id = "legacy-1",
                title = "Legacy remote episode",
                remoteId = "legacy-remote-1",
                audioUrl = "https://example.com/legacy.mp3",
            )
            insertEpisode(
                sqlite,
                id = "downloaded-1",
                title = "Downloaded episode",
                localUri = "content://auralis/downloads/downloaded-1",
                remoteId = "download-remote-1",
                audioUrl = "https://example.com/downloaded.mp3",
                isAiGenerated = false,
                lastPositionMs = 42_000L,
                lastPlayedAt = 1_700_000_000_123L,
            )
        } finally {
            sqlite.close()
        }
    }

    private fun insertEpisode(
        sqlite: SQLiteDatabase,
        id: String,
        title: String,
        localUri: String? = null,
        remoteId: String? = null,
        audioUrl: String? = null,
        isAiGenerated: Boolean = true,
        lastPositionMs: Long = 0L,
        lastPlayedAt: Long? = null,
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("podcastName", "Auralis")
            put("description", "migration test")
            put("durationMs", 120_000L)
            putNull("artworkUri")
            if (localUri == null) putNull("localUri") else put("localUri", localUri)
            if (remoteId == null) putNull("remoteId") else put("remoteId", remoteId)
            if (audioUrl == null) putNull("audioUrl") else put("audioUrl", audioUrl)
            put("isAiGenerated", if (isAiGenerated) 1 else 0)
            put("lastPositionMs", lastPositionMs)
            if (lastPlayedAt == null) putNull("lastPlayedAt") else put("lastPlayedAt", lastPlayedAt)
        }
        sqlite.insertOrThrow("episodes", null, values)
    }
}
