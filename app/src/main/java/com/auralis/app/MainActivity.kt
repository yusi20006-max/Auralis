package com.auralis.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.auralis.app.data.LocalAudioImport
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.app.data.CatalogConfig
import com.auralis.app.data.Episode
import com.auralis.app.data.LibraryRepository
import com.auralis.app.data.LocalUriAccess
import com.auralis.app.data.PlaybackPreferences
import com.auralis.app.data.catalog.CatalogRepository
import com.auralis.app.data.download.DownloadIdentity
import com.auralis.app.ui.components.AuralisMiniPlayer
import com.auralis.app.ui.components.MiniPlayerState
import com.auralis.app.ui.navigation.AuralisDestination
import com.auralis.app.ui.screens.CatalogPodcastDetailScreen
import com.auralis.app.ui.screens.ExploreScreen
import com.auralis.app.ui.screens.LibraryScreen
import com.auralis.app.ui.screens.NowPlayingScreen
import com.auralis.app.ui.screens.ProfileScreen
import com.auralis.app.ui.screens.SettingsScreen
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class MainActivity : ComponentActivity() {

    private lateinit var libraryRepository: LibraryRepository
    private lateinit var catalogRepository: CatalogRepository
    private lateinit var playbackPreferences: PlaybackPreferences
    private lateinit var localUriAccess: LocalUriAccess

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) { }
        lifecycleScope.launch {
            val imported = withContext(Dispatchers.IO) {
                LocalAudioImport.importOne(this@MainActivity, uri, libraryRepository)
            }
            libraryRepository.setCurrentEpisode(imported.id)
            playEpisode(imported)
        }
    }

    private val openAudioFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri: Uri? ->
        if (treeUri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) { }
        lifecycleScope.launch {
            val imported = withContext(Dispatchers.IO) {
                LocalAudioImport.importTree(this@MainActivity, treeUri, libraryRepository)
            }
            if (imported.isNotEmpty()) {
                libraryRepository.setCurrentEpisode(imported.first().id)
                playEpisode(imported.first())
            }
        }
    }

    private fun applyPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    private fun playEpisode(episode: Episode) {
        val c = controller ?: return
        val playable = localUriAccess.resolvePlayableUri(episode) ?: return
        val mediaItem = MediaItem.Builder()
            .setUri(playable)
            .setMediaId(episode.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(episode.podcastName)
                    .setArtworkUri(episode.artworkUri)
                    .build(),
            )
            .build()
        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()
        libraryRepository.setCurrentEpisode(episode.id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        libraryRepository = LibraryRepository.get(this)
        catalogRepository = CatalogRepository.get(this)
        playbackPreferences = PlaybackPreferences(this)
        localUriAccess = LocalUriAccess(this)

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            applyPlaybackSpeed(playbackPreferences.speed)
        }, MoreExecutors.directExecutor())

        setContent {
            AuralisTheme {
                var currentDestination by remember { mutableStateOf(AuralisDestination.Library) }
                var showNowPlaying by remember { mutableStateOf(false) }
                var selectedCatalogPodcastId by remember { mutableStateOf<String?>(null) }

                val episodes by libraryRepository.episodes.collectAsState()
                val currentId by libraryRepository.currentEpisodeId.collectAsState()
                val catalogSourceId = CatalogConfig.SOURCE_ID
                val downloadedIdentities by libraryRepository.downloadedIdentities.collectAsState()
                val downloadingIdentities by libraryRepository.downloadingIdentities.collectAsState()
                val downloadProgress by libraryRepository.downloadProgress.collectAsState()
                val downloadErrors by libraryRepository.downloadErrors.collectAsState()
                val libraryIdentities by libraryRepository.libraryIdentities.collectAsState()

                val isPlaying = controller?.isPlaying == true
                val currentEpisode = currentId?.let { id -> episodes.find { it.id == id } }

                BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
                BackHandler(enabled = selectedCatalogPodcastId != null) { selectedCatalogPodcastId = null }

                Surface(modifier = Modifier.fillMaxSize(), color = AuralisColors.BackgroundDeep) {
                    if (showNowPlaying && currentEpisode != null) {
                        NowPlayingScreen(
                            episode = currentEpisode,
                            isPlaying = isPlaying,
                            onPlayPause = {
                                if (isPlaying) controller?.pause() else controller?.play()
                            },
                            onSeek = { pos -> controller?.seekTo(pos) },
                            onBack = { showNowPlaying = false },
                            positionMs = controller?.currentPosition ?: 0L,
                            durationMs = controller?.duration?.takeIf { it > 0 } ?: currentEpisode.durationMs,
                            onSpeedChange = { speed ->
                                playbackPreferences.speed = speed
                                applyPlaybackSpeed(speed)
                            },
                            currentSpeed = playbackPreferences.speed,
                        )
                    } else if (selectedCatalogPodcastId != null) {
                        val podcast = catalogRepository.podcasts.value.find { it.id == selectedCatalogPodcastId }
                        if (podcast != null) {
                            CatalogPodcastDetailScreen(
                                podcast = podcast,
                                onBack = { selectedCatalogPodcastId = null },
                                onPlay = { ep ->
                                    val episode = libraryRepository.addOrGetStreamEpisode(ep, podcast)
                                    playEpisode(episode)
                                    showNowPlaying = true
                                },
                                onAddToLibrary = { ep -> libraryRepository.addToLibrary(ep, podcast) },
                                onDownload = { ep -> libraryRepository.downloadEpisode(ep, podcast) },
                                isDownloaded = { ep -> libraryRepository.isDownloaded(DownloadIdentity(catalogSourceId, ep.id)) },
                                isInLibrary = { ep -> libraryRepository.isInLibrary(DownloadIdentity(catalogSourceId, ep.id)) },
                            )
                        }
                    } else {
                        Scaffold(
                            containerColor = AuralisColors.BackgroundDeep,
                            bottomBar = {
                                Column {
                                    if (currentEpisode != null) {
                                        AuralisMiniPlayer(
                                            state = MiniPlayerState(
                                                title = currentEpisode.title,
                                                subtitle = currentEpisode.podcastName,
                                                isPlaying = isPlaying,
                                                progress = currentEpisode.progressFraction,
                                            ),
                                            onPlayPause = {
                                                if (isPlaying) controller?.pause() else controller?.play()
                                            },
                                            onClick = { showNowPlaying = true },
                                        )
                                    }
                                    NavigationBar(containerColor = AuralisColors.Surface) {
                                        AuralisDestination.entries.forEach { dest ->
                                            NavigationBarItem(
                                                selected = currentDestination == dest,
                                                onClick = { currentDestination = dest },
                                                icon = { Text(dest.label) },
                                                label = { Text(dest.label) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = AuralisColors.PrimaryBlue,
                                                    selectedTextColor = AuralisColors.PrimaryBlue,
                                                    unselectedIconColor = AuralisColors.TextTertiary,
                                                    unselectedTextColor = AuralisColors.TextTertiary,
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                        ) { padding ->
                            Box(Modifier.padding(padding).fillMaxSize()) {
                                when (currentDestination) {
                                    AuralisDestination.Library -> {
                                        LibraryScreen(
                                            episodes = episodes,
                                            onEpisodeClick = { episode ->
                                                playEpisode(episode)
                                                showNowPlaying = true
                                            },
                                            onEpisodeDelete = { episode ->
                                                libraryRepository.deleteEpisode(episode.id)
                                            },
                                            onImportClick = {
                                                openAudio.launch(arrayOf("audio/*"))
                                            },
                                            onImportFolderClick = {
                                                openAudioFolder.launch(null)
                                            },
                                        )
                                    }
                                    AuralisDestination.Explore -> {
                                        ExploreScreen(
                                            onImportClick = {
                                                openAudio.launch(arrayOf("audio/*"))
                                            },
                                            downloadedIdentities = downloadedIdentities,
                                            downloadingIdentities = downloadingIdentities,
                                            downloadProgress = downloadProgress,
                                            downloadErrors = downloadErrors,
                                            libraryIdentities = libraryIdentities,
                                            isDownloaded = libraryRepository::isDownloaded,
                                            onAddToLibrary = { podcast, catalogEpisode ->
                                                libraryRepository.addToLibrary(catalogEpisode, podcast)
                                            },
                                            onRemoveFromLibrary = { _, catalogEpisode ->
                                                libraryRepository.removeFromLibrary(
                                                    DownloadIdentity(catalogSourceId, catalogEpisode.id),
                                                )
                                            },
                                            onDownload = { podcast, catalogEpisode ->
                                                libraryRepository.downloadEpisode(catalogEpisode, podcast)
                                            },
                                            onCancelDownload = { identity ->
                                                libraryRepository.cancelDownload(identity)
                                            },
                                            onPlay = { podcast, catalogEpisode ->
                                                val episode = libraryRepository.addOrGetStreamEpisode(catalogEpisode, podcast)
                                                playEpisode(episode)
                                                showNowPlaying = true
                                            },
                                            onOpenPodcast = { podcast ->
                                                selectedCatalogPodcastId = podcast.id
                                            },
                                        )
                                    }
                                    AuralisDestination.Profile -> {
                                        ProfileScreen(episodeCount = episodes.size)
                                    }
                                    AuralisDestination.Settings -> {
                                        SettingsScreen()
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
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
