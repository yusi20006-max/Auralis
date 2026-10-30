package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.data.catalog.CatalogEpisode
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.catalog.CatalogRepository
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

@Composable
fun CatalogPodcastDetailScreen(
    podcast: CatalogPodcast,
    catalogRepository: CatalogRepository,
    downloadedRemoteIds: Set<String> = emptySet(),
    downloadingRemoteIds: Set<String> = emptySet(),
    onBack: () -> Unit,
    onPlayEpisode: (CatalogEpisode) -> Unit = {},
    onDownloadEpisode: (CatalogEpisode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val episodes by catalogRepository.observeEpisodes(podcast.id).collectAsState(initial = emptyList())

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
            TextButton(onClick = onBack) {
                Text("بازگشت", color = AuralisColors.TextSecondary)
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(AuralisCorners.Artwork))
                    .background(
                        Brush.linearGradient(
                            listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("A", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(AuralisSpacing.X3))
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
            text = "فایل‌های دانلودشده فقط داخل Auralis ذخیره می‌شوند و در مدیریت فایل گوشی دیده نمی‌شوند.",
            color = AuralisColors.TextTertiary,
            fontSize = AuralisTypeScale.Caption,
        )
        Spacer(modifier = Modifier.height(AuralisSpacing.X4))
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
        ) {
            items(episodes, key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    isDownloaded = downloadedRemoteIds.contains(episode.id),
                    isDownloading = downloadingRemoteIds.contains(episode.id),
                    onPlay = { onPlayEpisode(episode) },
                    onDownload = { onDownloadEpisode(episode) },
                )
                Spacer(modifier = Modifier.height(AuralisSpacing.X2))
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: CatalogEpisode,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val canStream = episode.audioUrl.isNotBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            Spacer(modifier = Modifier.height(AuralisSpacing.X2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onPlay,
                    enabled = canStream,
                ) {
                    Text(
                        text = if (isDownloaded) "▶ پخش آفلاین" else "▶ پخش آنلاین",
                        color = if (canStream) AuralisColors.PrimaryBlue else AuralisColors.TextTertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(
                    onClick = onDownload,
                    enabled = canStream && !isDownloaded && !isDownloading,
                ) {
                    Text(
                        text = when {
                            isDownloaded -> "✓ دانلود شده"
                            isDownloading -> "در حال دانلود…"
                            else -> "↓ دانلود خصوصی"
                        },
                        color = when {
                            isDownloaded -> AuralisColors.Success
                            isDownloading -> AuralisColors.AccentCyan
                            else -> AuralisColors.TextSecondary
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
