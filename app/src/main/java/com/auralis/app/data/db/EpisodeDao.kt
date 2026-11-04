package com.auralis.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes ORDER BY lastPlayedAt DESC, title ASC")
    fun observeAll(): Flow<List<EpisodeEntity>>

    @Query(
        "SELECT * FROM episodes WHERE isInLibrary = 1 " +
            "ORDER BY lastPlayedAt DESC, title ASC",
    )
    suspend fun getLibraryEpisodes(): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE sourceId = :sourceId AND remoteId = :remoteId LIMIT 1")
    suspend fun getBySourceAndRemoteId(sourceId: String, remoteId: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE localUri = :localUri LIMIT 1")
    suspend fun getByLocalUri(localUri: String): EpisodeEntity?

    /**
     * Updates only by the stable primary key. A new row is inserted with ABORT
     * semantics, so a collision on the secondary (sourceId, remoteId) identity
     * cannot REPLACE and delete an existing episode with its playback/download state.
     */
    @Transaction
    suspend fun upsert(episode: EpisodeEntity) {
        if (getById(episode.id) == null) {
            insert(episode)
        } else {
            update(episode)
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(episode: EpisodeEntity)

    @Update
    suspend fun update(episode: EpisodeEntity): Int

    /**
     * Serializes the lookup and insert in one Room transaction so concurrent
     * repository calls cannot both insert a row for the same local URI.
     * Re-import may fill artwork/duration that a previous pass missed.
     */
    @Transaction
    suspend fun getOrInsertLocalEpisode(episode: EpisodeEntity): EpisodeEntity {
        val localUri = requireNotNull(episode.localUri) {
            "A local episode must have a URI"
        }
        val existing = getByLocalUri(localUri)
        if (existing != null) {
            val merged = existing.copy(
                isInLibrary = true,
                artworkUri = episode.artworkUri ?: existing.artworkUri,
                durationMs = if (existing.durationMs > 0L) existing.durationMs else episode.durationMs,
                title = if (existing.title.isNotBlank() && existing.title != "اپیزود بدون عنوان") {
                    existing.title
                } else {
                    episode.title
                },
            )
            if (merged != existing) update(merged)
            return merged
        }
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
