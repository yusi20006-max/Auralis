package com.auralis.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes ORDER BY lastPlayedAt DESC, title ASC")
    fun observeAll(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(episode: EpisodeEntity)

    @Query("UPDATE episodes SET lastPositionMs = :positionMs, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun updatePosition(id: String, positionMs: Long, playedAt: Long)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun delete(id: String)
}
