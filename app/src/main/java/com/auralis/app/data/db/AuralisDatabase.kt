package com.auralis.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EpisodeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun episodeDao(): EpisodeDao

    companion object {
        @Volatile
        private var instance: AuralisDatabase? = null

        fun get(context: Context): AuralisDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuralisDatabase::class.java,
                    "auralis.db",
                ).build().also { instance = it }
            }
        }
    }
}
