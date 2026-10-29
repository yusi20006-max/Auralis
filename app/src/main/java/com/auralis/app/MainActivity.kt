package com.auralis.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.app.data.Episode
import com.auralis.app.data.LibraryRepository
import com.auralis.app.data.PlaybackPreferences
import com.auralis.app.ui.components.AuralisErrorBanner
import com.auralis.app.ui.components.AuralisMiniPlayer
import com.auralis.app.ui.components.ExploreNavIcon
import com.auralis.app.ui.components.LibraryNavIcon
import com.auralis.app.ui.components.MiniPlayerUiState
import com.auralis.app.ui.components.ProfileNavIcon
import com.auralis.app.ui.components.fileNameFromDocumentPath
import com.auralis.app.ui.components.resolveMiniPlayerTitle
import com.auralis.app.ui.components.resolveMiniPlayerUiState
import com.auralis.app.ui.navigation.AuralisDestination
import com.auralis.app.ui.screens.ExploreScreen
import com.auralis.app.ui.screens.LibraryScreen
import com.auralis.app.ui.screens.NowPlayingScreen
import com.auralis.app.ui.screens.ProfileScreen
import com.auralis.app.ui.screens.SettingsScreen
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTheme
import com.auralis.app.ui.theme.AuralisTypeScale
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

class MainActivity : ComponentActivity() {

    private lateinit var libraryRepository: LibraryRepository
    private lateinit var playbackPreferences: PlaybackPreferences

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var isPlaying by mutableStateOf(false)
    private var playerState by mutableStateOf(Player.STATE_IDLE)
    private var playWhenReady by mutableStateOf(false)
    private var hasMediaItem by mutableStateOf(false)
    private var hasPlayerError by mutableStateOf(false)
    private var sessionTitle by mutableStateOf<String?>(null)
    private var attachedFileName by mutableStateOf<String?>(null)
    private var positionMs by mutableStateOf(0L)
    private var durationMs by mutableStateOf(0L)

    private var currentDestination by mutableStateOf(AuralisDestination.Library)
    private var showNowPlaying by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)

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
                        } else null
                    }?.let { return it }
            } catch (_: Exception) { }
        }
        return fileNameFromDocumentPath(resolved.lastPathSegment)
    }

    private fun resolveDisplayNameOffMain(uri: Uri?, onResolved: (String?) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val name = displayNameFor(uri)
            withContext(Dispatchers.Main) { onResolved(name) }
        }
    }

    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }

        resolveDisplayNameOffMain(uri) { name ->
            val title = name ?: "فایل صوتی"
            val episode = libraryRepository.addLocalEpisode(uri = uri, title = title)
            libraryRepository.setCurrentEpisode(episode.id)
            playEpisode(episode)
        }
    }

    private fun applyPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    private fun playEpisode(episode: Episode) {
        val player = controller ?: return
        val uri = episode.localUri ?: return
        try {
            hasPlayerError = false
            player.pause()
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(episode.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episode.title)
                            .setArtist(episode.podcastName)
                            .build()
                    )
                    .build()
            )
            hasMediaItem = true
            player.prepare()
            if (episode.lastPositionMs > 0) {
                player.seekTo(episode.lastPositionMs)
            }
            applyPlaybackSpeed(playbackPreferences.speed.value)
            player.playWhenReady = true
            player.play()
            libraryRepository.setCurrentEpisode(episode.id)
        } catch (_: RuntimeException) {
            hasPlayerError = true
        }
    }

    private fun togglePlayback() {
        val player = controller ?: return
        val hasItem = player.currentMediaItem != null
        val endedWhileLoaded = hasItem && player.playbackState == Player.STATE_ENDED

        when {
            endedWhileLoaded -> {
                player.seekToDefaultPosition()
                player.play()
                hasPlayerError = false
            }
            player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING) -> {
                player.pause()
            }
            hasItem -> {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
                hasPlayerError = false
            }
        }
    }

    private fun seekTo(position: Long) {
        controller?.seekTo(position)
        positionMs = position
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        libraryRepository = LibraryRepository(applicationContext)
        playbackPreferences = PlaybackPreferences(applicationContext)

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                controller = future.get()
                controller?.let { connected ->
                    isPlaying = connected.isPlaying
                    hasPlayerError = connected.playerError != null
                    playerState = connected.playbackState
                    playWhenReady = connected.playWhenReady
                    hasMediaItem = connected.currentMediaItem != null
                    durationMs = connected.duration.coerceAtLeast(0L)
                    positionMs = connected.currentPosition.coerceAtLeast(0L)
                    applyPlaybackSpeed(playbackPreferences.speed.value)

                    connected.currentMediaItem?.localConfiguration?.uri?.let { uri ->
                        resolveDisplayNameOffMain(uri) { name -> attachedFileName = name }
                    }
                    sessionTitle = connected.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }

                    connected.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            playerState = playbackState
                            if (playbackState == Player.STATE_ENDED) isPlaying = false
                            if (playbackState == Player.STATE_READY) {
                                durationMs = connected.duration.coerceAtLeast(0L)
                            }
                        }

                        override fun onPlayWhenReadyChanged(ready: Boolean, reason: Int) {
                            playWhenReady = ready
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            hasMediaItem = mediaItem != null
                            mediaItem?.localConfiguration?.uri?.let { uri ->
                                resolveDisplayNameOffMain(uri) { name -> attachedFileName = name }
                            }
                            sessionTitle = mediaItem?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
                            applyPlaybackSpeed(playbackPreferences.speed.value)
                        }

                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            sessionTitle = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            isPlaying = false
                            hasPlayerError = true
                        }
                    })

                    lifecycleScope.launch {
                        while (isActive) {
                            val c = controller
                            if (c != null && c.isPlaying) {
                                positionMs = c.currentPosition.coerceAtLeast(0L)
                                durationMs = c.duration.coerceAtLeast(0L)
                                c.currentMediaItem?.mediaId?.let { id ->
                                    if (id.isNotBlank()) {
                                        libraryRepository.updatePosition(id, positionMs)
                                    }
                                }
                            }
                            delay(500)
                        }
                    }
                }
            } catch (_: ExecutionException) {
                hasPlayerError = true
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }, mainExecutor)

        setContent {
            AuralisTheme {
                val episodes by libraryRepository.episodes.collectAsState()
                val currentEpisodeId by libraryRepository.currentEpisodeId.collectAsState()
                val currentEpisode = episodes.find { it.id == currentEpisodeId }
                val playbackSpeed by playbackPreferences.speed.collectAsState()

                val miniState = resolveMiniPlayerUiState(
                    hasMediaItem = hasMediaItem,
                    hasPlayerError = hasPlayerError,
                    isBuffering = playerState == Player.STATE_BUFFERING,
                    hasEnded = playerState == Player.STATE_ENDED,
                    isPlaying = isPlaying,
                )
                val miniPlayerVisible = miniState != MiniPlayerUiState.NONE && !showNowPlaying && !showSettings
                val showPauseAction = isPlaying || (playWhenReady && playerState == Player.STATE_BUFFERING)
                val trackTitle = resolveMiniPlayerTitle(
                    hasMediaItem = hasMediaItem,
                    mediaMetadataTitle = sessionTitle,
                    pickedFileName = currentEpisode?.title ?: attachedFileName,
                )

                Surface(modifier = Modifier.fillMaxSize(), color = AuralisColors.BackgroundDeep) {
                    when {
                        showNowPlaying -> {
                            NowPlayingScreen(
                                episode = currentEpisode,
                                isPlaying = isPlaying,
                                positionMs = positionMs,
                                durationMs = durationMs,
                                playbackSpeed = playbackSpeed,
                                onSpeedChange = { speed ->
                                    playbackPreferences.setSpeed(speed)
                                    applyPlaybackSpeed(speed)
                                },
                                onPlayPause = { togglePlayback() },
                                onSeek = { seekTo(it) },
                                onClose = { showNowPlaying = false },
                            )
                        }
                        showSettings -> {
                            SettingsScreen(
                                playbackSpeed = playbackSpeed,
                                onSpeedChange = { speed ->
                                    playbackPreferences.setSpeed(speed)
                                    applyPlaybackSpeed(speed)
                                },
                                onBack = { showSettings = false },
                            )
                        }
                        else -> {
                            Scaffold(
                                containerColor = AuralisColors.BackgroundDeep,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                bottomBar = {
                                    Column {
                                        if (miniPlayerVisible) {
                                            val interactionSource = remember { MutableInteractionSource() }
                                            AuralisMiniPlayer(
                                                title = trackTitle,
                                                state = miniState,
                                                showPauseAction = showPauseAction,
                                                onPlayPause = { togglePlayback() },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = AuralisSpacing.X4, vertical = AuralisSpacing.X2)
                                                    .clickable(
                                                        interactionSource = interactionSource,
                                                        indication = null,
                                                        onClick = { showNowPlaying = true },
                                                    ),
                                            )
                                        }
                                        NavigationBar(containerColor = AuralisColors.Surface) {
                                            NavigationBarItem(
                                                selected = currentDestination == AuralisDestination.Library,
                                                onClick = { currentDestination = AuralisDestination.Library },
                                                icon = {
                                                    LibraryNavIcon(
                                                        color = navIconColor(currentDestination == AuralisDestination.Library),
                                                    )
                                                },
                                                label = { Text("کتابخانه", fontSize = AuralisTypeScale.Caption) },
                                                colors = navColors(),
                                            )
                                            NavigationBarItem(
                                                selected = currentDestination == AuralisDestination.Explore,
                                                onClick = { currentDestination = AuralisDestination.Explore },
                                                icon = {
                                                    ExploreNavIcon(
                                                        color = navIconColor(currentDestination == AuralisDestination.Explore),
                                                    )
                                                },
                                                label = { Text("کشف", fontSize = AuralisTypeScale.Caption) },
                                                colors = navColors(),
                                            )
                                            NavigationBarItem(
                                                selected = currentDestination == AuralisDestination.Profile,
                                                onClick = { currentDestination = AuralisDestination.Profile },
                                                icon = {
                                                    ProfileNavIcon(
                                                        color = navIconColor(currentDestination == AuralisDestination.Profile),
                                                    )
                                                },
                                                label = { Text("پروفایل", fontSize = AuralisTypeScale.Caption) },
                                                colors = navColors(),
                                            )
                                        }
                                    }
                                },
                            ) { padding ->
                                Column(Modifier.padding(padding).fillMaxSize()) {
                                    if (hasPlayerError) {
                                        AuralisErrorBanner(
                                            message = "پخش فایل ممکن نشد. فایل جابه‌جا شده یا دسترسی از بین رفته است.",
                                            actionLabel = "افزودن دوباره",
                                            onAction = { openAudio.launch(arrayOf("audio/*")) },
                                            modifier = Modifier.padding(
                                                horizontal = AuralisSpacing.X4,
                                                vertical = AuralisSpacing.X2,
                                            ),
                                        )
                                    }
                                    Box(Modifier.fillMaxSize()) {
                                        when (currentDestination) {
                                            AuralisDestination.Library,
                                            AuralisDestination.NowPlaying,
                                            AuralisDestination.EpisodeDetail -> {
                                                LibraryScreen(
                                                    episodes = episodes,
                                                    onEpisodeClick = { episode ->
                                                        libraryRepository.setCurrentEpisode(episode.id)
                                                        playEpisode(episode)
                                                        showNowPlaying = true
                                                    },
                                                    onEpisodeDelete = { episode ->
                                                        libraryRepository.deleteEpisode(episode.id)
                                                    },
                                                    onImportClick = { openAudio.launch(arrayOf("audio/*")) },
                                                )
                                            }
                                            AuralisDestination.Explore -> {
                                                ExploreScreen(
                                                    onImportClick = { openAudio.launch(arrayOf("audio/*")) },
                                                )
                                            }
                                            AuralisDestination.Profile -> {
                                                ProfileScreen(
                                                    episodeCount = episodes.size,
                                                    onOpenSettings = { showSettings = true },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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

private fun navIconColor(selected: Boolean): Color =
    if (selected) AuralisColors.PrimaryBlue else AuralisColors.TextTertiary

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AuralisColors.PrimaryBlue,
    selectedTextColor = AuralisColors.PrimaryBlue,
    indicatorColor = AuralisColors.SurfaceElevated,
    unselectedIconColor = AuralisColors.TextTertiary,
    unselectedTextColor = AuralisColors.TextTertiary,
)
