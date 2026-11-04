package com.auralis.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.auralis.app.data.db.AuralisDatabase
import com.auralis.app.data.db.EpisodeEntity
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background playback with notification + lock-screen controls (Media3 library session).
 *
 * Android Auto:
 * - [MediaLibraryService] + [MediaLibrarySession] for system/Auto browse and play (#40).
 * - Library tree: root → کتابخانه → isInLibrary episodes from Room (#41).
 * - Browse/play does **not** create Library membership (architecture #24).
 *
 * Issue #12 audit (code-side):
 * - Audio focus: handleAudioFocus = true
 * - Headset unplug / BT disconnect: setHandleAudioBecomingNoisy(true) → pause
 * - Screen-off: WAKE_MODE_LOCAL while playing
 * - Notification / lock screen: MediaSession + sessionActivity → MainActivity
 * - Task removed from recents: keep service if playWhenReady so audio continues
 */
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao by lazy { AuralisDatabase.get(this).episodeDao() }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return suspendFuture {
                when (parentId) {
                    LIBRARY_ROOT_ID -> {
                        LibraryResult.ofItemList(ImmutableList.of(libraryFolderItem()), params)
                    }
                    LIBRARY_FOLDER_ID -> {
                        val episodes = dao.getLibraryEpisodes()
                        val start = (page * pageSize).coerceAtMost(episodes.size)
                        val end = (start + pageSize).coerceAtMost(episodes.size)
                        val pageItems = episodes.subList(start, end).map { it.toBrowsableMediaItem() }
                        LibraryResult.ofItemList(ImmutableList.copyOf(pageItems), params)
                    }
                    else -> LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return suspendFuture {
                when {
                    mediaId == LIBRARY_ROOT_ID -> LibraryResult.ofItem(rootItem(), null)
                    mediaId == LIBRARY_FOLDER_ID -> LibraryResult.ofItem(libraryFolderItem(), null)
                    mediaId.startsWith(EPISODE_ID_PREFIX) -> {
                        val id = mediaId.removePrefix(EPISODE_ID_PREFIX)
                        val entity = dao.getById(id)
                        if (entity != null) {
                            LibraryResult.ofItem(entity.toBrowsableMediaItem(), null)
                        } else {
                            LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                        }
                    }
                    else -> LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
            }
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return suspendFuture {
                val resolved = mediaItems.map { resolvePlayable(it) }
                MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
            }
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            return suspendFuture {
                mediaItems.map { resolvePlayable(it) }.toMutableList()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.repeatMode = Player.REPEAT_MODE_OFF

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, libraryCallback)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        val keepAlive = player != null &&
            (player.isPlaying || player.playWhenReady) &&
            player.playbackState != Player.STATE_ENDED &&
            player.playbackState != Player.STATE_IDLE
        if (!keepAlive) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    private suspend fun resolvePlayable(item: MediaItem): MediaItem {
        val mediaId = item.mediaId ?: return item
        if (!mediaId.startsWith(EPISODE_ID_PREFIX)) return item
        val id = mediaId.removePrefix(EPISODE_ID_PREFIX)
        val entity = dao.getById(id) ?: return item
        val playUri = entity.localUri?.let { Uri.parse(it) }
            ?: entity.audioUrl?.let { Uri.parse(it) }
            ?: return item
        return entity.toPlayableMediaItem(playUri)
    }

    private fun <T> suspendFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        serviceScope.launch {
            try {
                future.set(block())
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        return future
    }

    companion object {
        const val LIBRARY_ROOT_ID = "auralis_root"
        const val LIBRARY_FOLDER_ID = "auralis_library"
        const val EPISODE_ID_PREFIX = "episode:"

        private fun rootItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(LIBRARY_ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Auralis")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()

        private fun libraryFolderItem(): MediaItem =
            MediaItem.Builder()
                .setMediaId(LIBRARY_FOLDER_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("کتابخانه")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()

        private fun EpisodeEntity.toBrowsableMediaItem(): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(podcastName)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .apply {
                    if (durationMs > 0L) setDurationMs(durationMs)
                    artworkUri?.let { setArtworkUri(Uri.parse(it)) }
                }
                .build()
            return MediaItem.Builder()
                .setMediaId(EPISODE_ID_PREFIX + id)
                .setMediaMetadata(metadata)
                .build()
        }

        private fun EpisodeEntity.toPlayableMediaItem(playUri: Uri): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(podcastName)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .apply {
                    if (durationMs > 0L) setDurationMs(durationMs)
                    artworkUri?.let { setArtworkUri(Uri.parse(it)) }
                }
                .build()
            return MediaItem.Builder()
                .setMediaId(EPISODE_ID_PREFIX + id)
                .setUri(playUri)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
