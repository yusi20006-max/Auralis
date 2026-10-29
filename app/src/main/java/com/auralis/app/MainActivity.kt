package com.auralis.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import com.auralis.app.ui.components.AuralisMiniPlayer
import com.auralis.app.ui.components.MiniPlayerUiState
import com.auralis.app.ui.components.fileNameFromDocumentPath
import com.auralis.app.ui.components.resolveMiniPlayerTitle
import com.auralis.app.ui.components.resolveMiniPlayerUiState
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTheme
import com.auralis.app.ui.theme.AuralisTypeScale

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingUri: Uri? = null
    private var selectedTitle by mutableStateOf("هنوز فایلی انتخاب نشده")
    private var isPlaying by mutableStateOf(false)
    private var status by mutableStateOf("یک فایل صوتی از حافظهٔ گوشی انتخاب کنید.")

    // Session facts mirrored for the mini player. Every one of these is read back from the same
    // MediaController that drives the existing screen; none of them is set optimistically.
    private var playerState by mutableStateOf(Player.STATE_IDLE)
    private var playWhenReady by mutableStateOf(false)
    private var hasMediaItem by mutableStateOf(false)
    private var hasPlayerError by mutableStateOf(false)
    private var sessionTitle by mutableStateOf<String?>(null)
    private var attachedFileName by mutableStateOf<String?>(null)

    /**
     * Display name of a document URI; null when the URI carries no usable name.
     *
     * The URI segment is only a hint: for MediaDocumentsProvider, `document/audio%3A1234`
     * yields the opaque id `audio:1234`, whose numeric tail is not a file name. The provider
     * owns the human-readable name, so ask for it first and fall back to parsing the segment
     * only when the provider cannot answer (`file:` URIs, providers without the column).
     *
     * The query is read-only metadata and does not touch the persisted grant.
     */
    private fun displayNameFor(uri: Uri?): String? {
        val resolved = uri ?: return null
        if (resolved.scheme == "content") {
            try {
                contentResolver
                    .query(resolved, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (column != -1 && cursor.moveToFirst()) {
                            cursor.getString(column)?.takeIf { it.isNotBlank() }
                        } else {
                            null
                        }
                    }
                    ?.let { return it }
            } catch (_: Exception) {
                // The provider refused the query; fall through to the URI-segment fallback.
            }
        }
        return fileNameFromDocumentPath(resolved.lastPathSegment)
    }

    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // The temporary read grant from OpenDocument remains usable for this activity session.
            } catch (_: IllegalArgumentException) {
                // Some document providers do not offer persistable grants.
            }
            pendingUri = uri
            selectedTitle = displayNameFor(uri) ?: "فایل صوتی انتخاب‌شده"
            startSelectedFileIfReady()
        }
    }

    private fun startSelectedFileIfReady() {
        val player = controller
        val uri = pendingUri
        if (player == null || uri == null) {
            status = "فایل انتخاب شد؛ در انتظار آماده‌شدن پخش‌کننده…"
            return
        }
        try {
            status = "در حال آماده‌سازی فایل…"
            isPlaying = false
            hasPlayerError = false
            player.pause()
            player.setMediaItem(MediaItem.fromUri(uri))
            // Only after setMediaItem returned: on the failure path below no media is loaded,
            // so the bar must not claim otherwise.
            hasMediaItem = true
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (error: RuntimeException) {
            isPlaying = false
            status = "شروع پخش ناموفق بود: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    /**
     * The one toggle used by both the original control and the mini player. Extracted verbatim so
     * both entry points drive the very same existing session.
     */
    private fun togglePlayback() {
        controller?.let {
            val hasItem = it.currentMediaItem != null
            val endedWhileLoaded = hasItem && it.playbackState == Player.STATE_ENDED
            if (endedWhileLoaded) {
                // Media3's play() sets the play intent but does not restart an ended item, so seek
                // to the current item's default position first; otherwise the session stays ENDED
                // and cannot resume. ENDED is checked before the play/pause branch because Media3
                // can keep playWhenReady true while the player reports ENDED.
                it.seekToDefaultPosition()
                it.play()
                status = "درخواست ادامهٔ پخش ارسال شد…"
            } else if (it.isPlaying || it.playWhenReady) {
                it.pause()
                status = "پخش مکث شد"
            } else if (hasItem) {
                // Stop() leaves the player in STATE_IDLE while retaining the item; play() alone
                // cannot leave IDLE, so prepare before asking to play.
                if (it.playbackState == Player.STATE_IDLE) {
                    it.prepare()
                }
                it.play()
                status = "درخواست ادامهٔ پخش ارسال شد…"
            } else {
                startSelectedFileIfReady()
            }
        } ?: run { status = "پخش‌کننده هنوز متصل نشده است." }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get()
                controller?.let { connected ->
                    // Seed the mirrored facts from the session before listening, so a re-attached
                    // controller never renders a stale or guessed state. isPlaying and the retained
                    // error are seeded too: a recreated activity must not report PAUSED while the
                    // service still plays, nor hide an error Media3 does not replay as a callback.
                    isPlaying = connected.isPlaying
                    hasPlayerError = connected.playerError != null
                    playerState = connected.playbackState
                    playWhenReady = connected.playWhenReady
                    hasMediaItem = connected.currentMediaItem != null
                    attachedFileName = displayNameFor(connected.currentMediaItem?.localConfiguration?.uri)
                    sessionTitle = connected.mediaMetadata.title
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                }
                controller?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) {
                            status = "در حال پخش"
                        } else if (controller?.playWhenReady == true &&
                            controller?.playbackState == Player.STATE_BUFFERING
                        ) {
                            status = "در حال بارگذاری صدا…"
                        } else if (controller?.playbackState == Player.STATE_ENDED) {
                            status = "پخش فایل به پایان رسید."
                        } else if (controller?.playWhenReady == false && status == "در حال پخش") {
                            status = "پخش متوقف است"
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        playerState = playbackState
                        when (playbackState) {
                            Player.STATE_BUFFERING -> if (controller?.playWhenReady == true) {
                                status = "در حال بارگذاری صدا…"
                            }
                            Player.STATE_READY -> if (controller?.playWhenReady == true && !isPlaying) {
                                status = "فایل آماده است؛ منتظر شروع صدا…"
                            }
                            Player.STATE_ENDED -> {
                                isPlaying = false
                                status = "پخش فایل به پایان رسید."
                            }
                        }
                    }

                    override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) {
                        playWhenReady = ready
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        hasMediaItem = mediaItem != null
                        attachedFileName = displayNameFor(mediaItem?.localConfiguration?.uri)
                        sessionTitle = mediaItem?.mediaMetadata?.title
                            ?.toString()
                            ?.takeIf { it.isNotBlank() }
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        // Combined player metadata, not just the MediaItem's: this is where
                        // titles parsed from the file itself (ID3 and friends) arrive, and it
                        // fires independently of onMediaItemTransition.
                        sessionTitle = mediaMetadata.title
                            ?.toString()
                            ?.takeIf { it.isNotBlank() }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        isPlaying = false
                        hasPlayerError = true
                        status = "خطای پخش (${error.errorCodeName}): ${error.message ?: "قالب فایل پشتیبانی نمی‌شود"}"
                    }
                })
                startSelectedFileIfReady()
            } catch (error: ExecutionException) {
                status = "اتصال به پخش‌کننده ناموفق بود: ${error.cause?.message ?: error.message}"
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                status = "آماده‌سازی پخش‌کننده متوقف شد."
            }
        }, mainExecutor)

        setContent {
            val background = Brush.verticalGradient(listOf(AuralisColors.BackgroundDeep, AuralisColors.Surface, AuralisColors.BackgroundDeep))
            AuralisTheme {
                val isSessionPlaying = isPlaying
                val miniState = resolveMiniPlayerUiState(
                    hasMediaItem = hasMediaItem,
                    hasPlayerError = hasPlayerError,
                    isBuffering = playerState == Player.STATE_BUFFERING,
                    hasEnded = playerState == Player.STATE_ENDED,
                    isPlaying = isSessionPlaying,
                )
                val miniPlayerVisible = miniState != MiniPlayerUiState.NONE
                // Pause glyph only while the session really is playing, or is buffering with
                // playWhenReady set. Anything else honestly offers "play".
                val showPauseAction = isSessionPlaying ||
                    (playWhenReady && playerState == Player.STATE_BUFFERING)
                val trackTitle = resolveMiniPlayerTitle(
                    hasMediaItem = hasMediaItem,
                    mediaMetadataTitle = sessionTitle,
                    pickedFileName = selectedTitle
                        .takeIf { it.isNotBlank() && it != "هنوز فایلی انتخاب نشده" }
                        ?: attachedFileName,
                )

                // Reserve the bar band before centring, so the opaque bar can never sit over
                // the controls, and scroll the column so nothing is ever clipped on a short
                // window, split-screen or a large font scale. With the bar hidden this is the
                // exact padding the screen had on main.
                val bottomInset = if (miniPlayerVisible) AuralisSpacing.X6 + AuralisSpacing.MiniPlayerMaxHeight else AuralisSpacing.X6

                Surface(Modifier.fillMaxSize(), color = AuralisColors.BackgroundDeep) {
                    Box(Modifier.fillMaxSize().background(background)) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(start = AuralisSpacing.X6, top = AuralisSpacing.X6, end = AuralisSpacing.X6, bottom = bottomInset),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AURALIS", color = AuralisColors.AccentCyan, fontSize = AuralisTypeScale.HeroMax, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                                Spacer(Modifier.height(6.dp))
                                Text("OFFLINE AUDIO", color = AuralisColors.PrimaryViolet, fontSize = AuralisTypeScale.Overline, letterSpacing = 3.sp)
                                Spacer(Modifier.height(AuralisSpacing.X8 + AuralisSpacing.X2))
                                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = AuralisColors.Surface) {
                                    Column(Modifier.padding(AuralisSpacing.X6), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("پخش‌کنندهٔ آفلاین", color = AuralisColors.TextPrimary, fontSize = AuralisTypeScale.TitleLargeMin, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(16.dp))
                                        Text(selectedTitle, color = AuralisColors.TextPrimary, textAlign = TextAlign.Center, fontSize = AuralisTypeScale.BodyMedium)
                                        Spacer(Modifier.height(10.dp))
                                        Text(status, color = AuralisColors.TextSecondary, textAlign = TextAlign.Center, fontSize = AuralisTypeScale.BodyMedium)
                                        Spacer(Modifier.height(24.dp))
                                        Button(onClick = { openAudio.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AuralisColors.PrimaryBlue, contentColor = AuralisColors.BackgroundDeep)) { Text("انتخاب فایل صوتی") }
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Button(onClick = { togglePlayback() }, colors = ButtonDefaults.buttonColors(containerColor = AuralisColors.PrimaryViolet, contentColor = AuralisColors.BackgroundDeep)) { Text(if (isPlaying) "مکث" else "ادامهٔ پخش") }
                                            Button(onClick = {
                                                controller?.stop()
                                                isPlaying = false
                                                status = "پخش متوقف شد"
                                            }, colors = ButtonDefaults.buttonColors(containerColor = AuralisColors.SurfaceElevated)) { Text("توقف") }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(22.dp))
                                Text("نسخهٔ آزمایشی · بدون سرور و حساب کاربری", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.Caption, textAlign = TextAlign.Center)
                            }
                        }
                        if (miniPlayerVisible) {
                            // Sits inside the band bottomInset reserved above, so it can never
                            // cover the controls; the column scrolls so a short window, split-screen
                            // or a large font scale cannot clip them either.
                            AuralisMiniPlayer(
                                title = trackTitle,
                                state = miniState,
                                showPauseAction = showPauseAction,
                                onPlayPause = { togglePlayback() },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(AuralisSpacing.X6),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onDestroy()
    }
}
