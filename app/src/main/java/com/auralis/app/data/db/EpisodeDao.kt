package com.auralis.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes ORDER BY lastPlayedAt DESC, title ASC")
    fun observeAll(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE localUri = :localUri LIMIT 1")
    suspend fun getByLocalUri(localUri: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(episode: EpisodeEntity)

    /**
     * Serializes the lookup and insert in one Room transaction so concurrent
     * repository calls cannot both insert a row for the same local URI.
     */
    @Transaction
    suspend fun getOrInsertLocalEpisode(episode: EpisodeEntity): EpisodeEntity {
        val localUri = requireNotNull(episode.localUri) {
            "A local episode must have a URI"
        }
        val existing = getByLocalUri(localUri)
        if (existing != null) return existing
        upsert(episode)
        return episode
    }

    @Query("UPDATE episodes SET lastPositionMs = :positionMs, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun updatePosition(id: String, positionMs: Long, playedAt: Long)

    @Query("UPDATE episodes SET localUri = :localUri WHERE id = :id")
    suspend fun updateLocalUri(id: String, localUri: String?)

    @Query("DELETE FROM episodes WHERE id = :id")
    suspend fun delete(id: String)
}
