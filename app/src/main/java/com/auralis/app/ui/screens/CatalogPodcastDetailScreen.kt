package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.data.catalog.CatalogConfig
import com.auralis.app.data.catalog.CatalogEpisode
import com.auralis.app.data.catalog.CatalogLoadState
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.catalog.CatalogRepository
import com.auralis.app.data.download.DownloadIdentity
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CatalogPodcastDetailScreen(
    podcast: CatalogPodcast,
    catalogRepository: CatalogRepository,
    downloadedIdentities: Set<DownloadIdentity> = emptySet(),
    downloadingIdentities: Set<DownloadIdentity> = emptySet(),
    downloadProgress: Map<DownloadIdentity, Float> = emptyMap(),
    downloadErrors: Map<DownloadIdentity, String> = emptyMap(),
    libraryIdentities: Set<DownloadIdentity> = emptySet(),
    isDownloaded: (DownloadIdentity) -> Boolean = { downloadedIdentities.contains(it) },
    loadState: CatalogLoadState = catalogRepository.loadState.collectAsState().value,
    onRetryCatalog: () -> Unit = {},
    onAddToLibrary: suspend (CatalogEpisode) -> Unit = {},
    onRemoveFromLibrary: suspend (CatalogEpisode) -> Unit = {},
    onBack: () -> Unit,
    onPlayEpisode: (CatalogEpisode) -> Unit = {},
    onDownloadEpisode: (CatalogEpisode) -> Unit = {},
    onCancelDownload: (CatalogEpisode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val episodes by catalogRepository.observeEpisodes(podcast.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var pendingLibraryActions by remember { mutableStateOf(emptySet<DownloadIdentity>()) }
    var libraryActionErrors by remember { mutableStateOf(emptyMap<DownloadIdentity, String>()) }

    fun runLibraryAction(identity: DownloadIdentity, action: suspend () -> Unit) {
        if (identity in pendingLibraryActions) return
        pendingLibraryActions = pendingLibraryActions + identity
        scope.launch {
            try {
                action()
                libraryActionErrors = libraryActionErrors - identity
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                libraryActionErrors = libraryActionErrors +
                    (identity to "تغییر عضویت کتابخانه انجام نشد؛ دوباره تلاش کنید.")
            } finally {
                pendingLibraryActions = pendingLibraryActions - identity
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisColors.BackgroundDeep)
            .padding(horizontal = AuralisSpacing.X4),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AuralisSpacing.X3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "بازگشت به فهرست کاوش" },
            ) {
                Text("بازگشت", color = AuralisColors.TextSecondary)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            PodcastArtwork(
                coverUrl = podcast.coverUrl,
                title = podcast.title,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.size(AuralisSpacing.X3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    color = AuralisColors.TextPrimary,
                    fontSize = AuralisTypeScale.TitleMediumMax,
                    fontWeight = FontWeight.SemiBold,
                )
                if (podcast.isAiGenerated) {
                    Text(
                        text = "AI Generated",
                        color = AuralisColors.AccentCyan,
                        fontSize = AuralisTypeScale.Caption,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AuralisSpacing.X3))
        Text(
            text = podcast.description,
            color = AuralisColors.TextSecondary,
            fontSize = AuralisTypeScale.BodyMedium,
        )
        Spacer(modifier = Modifier.height(AuralisSpacing.X2))
        Text(
            text = "دانلود خصوصی فقط داخل Auralis ذخیره می‌شود.",
            color = AuralisColors.TextTertiary,
            fontSize = AuralisTypeScale.Caption,
        )
        Spacer(modifier = Modifier.height(AuralisSpacing.X3))
        CatalogLoadStatus(loadState = loadState, onRetry = onRetryCatalog)
        Spacer(modifier = Modifier.height(AuralisSpacing.X2))
        Text(
            text = "اپیزودها",
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.TitleMediumMin,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(AuralisSpacing.X2))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
        ) {
            if (episodes.isEmpty()) {
                item {
                    Text(
                        text = "برای این پادکست هنوز اپیزودی ثبت نشده است.",
                        color = AuralisColors.TextSecondary,
                        fontSize = AuralisTypeScale.BodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AuralisSpacing.X8),
                    )
                }
            } else {
                items(episodes, key = { it.id }) { episode ->
                    val identity = DownloadIdentity(CatalogConfig.SOURCE_ID, episode.id)
                    val member = libraryIdentities.contains(identity)
                    val downloaded = downloadedIdentities.contains(identity) || isDownloaded(identity)
                    EpisodeRow(
                        episode = episode,
                        isDownloaded = downloaded,
                        isDownloading = downloadingIdentities.contains(identity),
                        progress = downloadProgress[identity],
                        downloadError = downloadErrors[identity],
                        isInLibrary = member,
                        isLibraryActionPending = identity in pendingLibraryActions,
                        libraryActionError = libraryActionErrors[identity],
                        onPlay = { onPlayEpisode(episode) },
                        onDownload = { onDownloadEpisode(episode) },
                        onCancel = { onCancelDownload(episode) },
                        onAddToLibrary = {
                            runLibraryAction(identity) { onAddToLibrary(episode) }
                        },
                        onRemoveFromLibrary = {
                            runLibraryAction(identity) { onRemoveFromLibrary(episode) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: CatalogEpisode,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    progress: Float?,
    downloadError: String?,
    isInLibrary: Boolean,
    isLibraryActionPending: Boolean,
    libraryActionError: String?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onAddToLibrary: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
) {
    val canStream = episode.audioUrl.isNotBlank()
    val canPlay = canStream || isDownloaded
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(episode.title)
                    when {
                        isDownloaded -> append(". فایل آفلاین آماده است")
                        isDownloading -> append(". در حال دانلود")
                        else -> append(". قابل پخش آنلاین یا دانلود خصوصی")
                    }
                    if (isInLibrary) append(". عضو کتابخانه")
                }
            },
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
    ) {
        Column(modifier = Modifier.padding(AuralisSpacing.X4)) {
            Text(
                text = episode.title,
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.BodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.description,
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.Caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(AuralisSpacing.X2))
                val value = progress?.coerceIn(0f, 1f)
                if (value != null && value > 0f) {
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "پیشرفت دانلود " + (value * 100).toInt() + " درصد" },
                        color = AuralisColors.PrimaryBlue,
                        trackColor = AuralisColors.SurfaceElevated,
                    )
                    Text(
                        text = "دانلود " + (value * 100).toInt() + "٪",
                        color = AuralisColors.AccentCyan,
                        fontSize = AuralisTypeScale.Caption,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "در حال دانلود" },
                        color = AuralisColors.PrimaryBlue,
                        trackColor = AuralisColors.SurfaceElevated,
                    )
                    Text(
                        text = "در حال دانلود…",
                        color = AuralisColors.AccentCyan,
                        fontSize = AuralisTypeScale.Caption,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AuralisSpacing.X2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPlay,
                    enabled = canPlay,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = if (isDownloaded) {
                                "پخش آفلاین " + episode.title
                            } else {
                                "پخش آنلاین " + episode.title
                            }
                        },
                ) {
                    Text(
                        text = if (isDownloaded) "▶ پخش آفلاین" else "▶ پخش آنلاین",
                        color = if (canPlay) AuralisColors.PrimaryBlue else AuralisColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(
                    onClick = if (isInLibrary) onRemoveFromLibrary else onAddToLibrary,
                    enabled = !isLibraryActionPending,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = when {
                                isLibraryActionPending -> "در حال تغییر عضویت " + episode.title
                                isInLibrary -> "حذف عضویت " + episode.title + " از کتابخانه"
                                else -> "افزودن " + episode.title + " به کتابخانه"
                            }
                        },
                ) {
                    Text(
                        text = when {
                            isLibraryActionPending -> "در حال ذخیره…"
                            libraryActionError != null -> "تلاش دوباره"
                            isInLibrary -> "حذف عضویت"
                            else -> "افزودن به کتابخانه"
                        },
                        color = if (isInLibrary) AuralisColors.Error else AuralisColors.AccentCyan,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (libraryActionError != null) {
                Text(
                    text = libraryActionError,
                    color = AuralisColors.Error,
                    fontSize = AuralisTypeScale.Caption,
                )
            } else if (isInLibrary && isDownloaded) {
                Text(
                    text = "حذف عضویت، فایل آفلاین را حفظ می‌کند.",
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Caption,
                )
            }

            Spacer(modifier = Modifier.height(AuralisSpacing.Base))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = if (isDownloading) onCancel else onDownload,
                    enabled = isDownloading || (canStream && !isDownloaded),
                    modifier = Modifier.semantics {
                        contentDescription = when {
                            isDownloading -> "لغو دانلود " + episode.title
                            downloadError != null -> "تلاش دوباره برای دانلود " + episode.title
                            isDownloaded -> "فایل آفلاین " + episode.title + " آماده است"
                            else -> "دانلود خصوصی " + episode.title
                        }
                    },
                ) {
                    Text(
                        text = when {
                            isDownloading -> "لغو دانلود"
                            downloadError != null -> "تلاش دوباره"
                            isDownloaded -> "✓ فایل آفلاین آماده است"
                            else -> "↓ دانلود خصوصی"
                        },
                        color = when {
                            isDownloading -> AuralisColors.Error
                            isDownloaded -> AuralisColors.Success
                            else -> AuralisColors.TextSecondary
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (downloadError != null) {
                    Text(
                        text = downloadError,
                        color = AuralisColors.Error,
                        fontSize = AuralisTypeScale.Caption,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
