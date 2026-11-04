package com.auralis.app

import android.app.PendingIntent
import android.content.Intent
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
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Background playback with notification + lock-screen controls (Media3 library session).
 *
 * Android Auto (#40):
 * - Service is [MediaLibraryService] so Auto/system can connect for browse + play.
 * - Stub library callback exposes a browsable root; episode children come in #41.
 *
 * Issue #12 audit (code-side):
 * - Audio focus: handleAudioFocus = true
 * - Headset unplug / BT disconnect: setHandleAudioBecomingNoisy(true) → pause
 * - Screen-off: WAKE_MODE_LOCAL while playing
 * - Notification / lock screen: MediaSession + sessionActivity → MainActivity
 * - Playing UI must mirror Player.isPlaying (MainActivity / MiniPlayer), never optimistic click
 * - Task removed from recents: keep service if playWhenReady so audio continues
 */
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(LIBRARY_ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Auralis")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // Episode listing from Room is implemented in #41.
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.of(), params),
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            if (mediaId == LIBRARY_ROOT_ID) {
                val root = MediaItem.Builder()
                    .setMediaId(LIBRARY_ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Auralis")
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build(),
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(root, /* params= */ null))
            }
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
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

    /**
     * If the user swipes the app away while audio should still play, keep the service.
     * Otherwise stop so we do not leak a silent session.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        val keepAlive = player != null &&
            (player.isPlaying || player.playWhenReady) &&
            player.playbackState != Player.STATE_ENDED &&
            player.playbackState != Player.STATE_IDLE
        if (!keepAlive) {
            stopSelf()
        }
        // When keepAlive: do not call stopSelf(); MediaLibraryService stays for notification controls.
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    companion object {
        const val LIBRARY_ROOT_ID = "auralis_root"
    }
}
