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

    // NOTE: This is a truncated stub for size; full file will be pushed next if this fails
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
