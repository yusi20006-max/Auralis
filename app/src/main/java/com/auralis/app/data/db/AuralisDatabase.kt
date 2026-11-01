package com.auralis.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EpisodeEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun episodeDao(): EpisodeDao

    companion object {
        @Volatile
        private var instance: AuralisDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE episodes ADD COLUMN sourceId TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_episodes_sourceId_remoteId " +
                        "ON episodes (sourceId, remoteId)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Existing rows were visible in Library before membership became explicit.
                // Preserve that behavior while future download-only rows opt out.
                db.execSQL("ALTER TABLE episodes ADD COLUMN isInLibrary INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun get(context: Context): AuralisDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuralisDatabase::class.java,
                    "auralis.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
