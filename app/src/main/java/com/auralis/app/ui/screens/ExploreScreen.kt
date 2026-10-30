package com.auralis.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val categories = listOf("همه", "فلسفه", "علم", "تکنولوژی")

@Composable
fun ExploreScreen(
    onImportClick: () -> Unit,
    onStreamEpisode: (CatalogPodcast, CatalogEpisode) -> Unit = { _, _ -> },
    onDownloadEpisode: (CatalogPodcast, CatalogEpisode) -> Unit = { _, _ -> },
    downloadedRemoteIds: Set<String> = emptySet(),
    downloadingRemoteIds: Set<String> = emptySet(),
    downloadProgress: Map<String, Float> = emptyMap(),
    catalogRepository: CatalogRepository = remember { CatalogRepository() },
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf("همه") }
    var selectedPodcast by remember { mutableStateOf<CatalogPodcast?>(null) }
    val podcasts by catalogRepository.observePodcasts(selectedCategory)
        .collectAsState(initial = emptyList())

    val podcast = selectedPodcast
    if (podcast != null) {
        BackHandler { selectedPodcast = null }
        CatalogPodcastDetailScreen(
            podcast = podcast,
            catalogRepository = catalogRepository,
            downloadedRemoteIds = downloadedRemoteIds,
            downloadingRemoteIds = downloadingRemoteIds,
            downloadProgress = downloadProgress,
            onBack = { selectedPodcast = null },
            onPlayEpisode = { episode -> onStreamEpisode(podcast, episode) },
            onDownloadEpisode = { episode -> onDownloadEpisode(podcast, episode) },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisColors.BackgroundDeep)
            .padding(horizontal = AuralisSpacing.X4),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item {
            Text(
                text = "کشف",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleLargeMax,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = AuralisSpacing.X4),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
            ) {
                categories.forEach { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(text = category, fontSize = AuralisTypeScale.Caption)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuralisColors.PrimaryBlue,
                            selectedLabelColor = AuralisColors.BackgroundDeep,
                            containerColor = AuralisColors.SurfaceElevated,
                            labelColor = AuralisColors.TextSecondary,
                        ),
                        shape = RoundedCornerShape(AuralisCorners.Button),
                    )
                }
            }
            Spacer(modifier = Modifier.height(AuralisSpacing.X6))
        }

        item {
            Text(
                text = "کاتالوگ AI",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMin,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(AuralisSpacing.X2))
            Text(
                text = "پخش آنلاین یا دانلود خصوصی (مخفی از مدیریت فایل).",
                color = AuralisColors.TextTertiary,
                fontSize = AuralisTypeScale.Caption,
            )
            Spacer(modifier = Modifier.height(AuralisSpacing.X3))
        }

        items(podcasts, key = { it.id }) { item ->
            CatalogPodcastRow(
                podcast = item,
                onClick = { selectedPodcast = item },
            )
            Spacer(modifier = Modifier.height(AuralisSpacing.X3))
        }

        item {
            Spacer(modifier = Modifier.height(AuralisSpacing.X4))
            Text(
                text = "شروع با فایل محلی",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMin,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(AuralisSpacing.X3))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AuralisCorners.Card))
                    .clickable(onClick = onImportClick),
                color = AuralisColors.Surface,
                shape = RoundedCornerShape(AuralisCorners.Card),
            ) {
                Column(
                    modifier = Modifier.padding(AuralisSpacing.X4),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "افزودن پادکست از حافظه",
                        color = AuralisColors.TextPrimary,
                        fontSize = AuralisTypeScale.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "فایل‌های صوتی AI خود را وارد کنید و آفلاین گوش دهید",
                        color = AuralisColors.TextSecondary,
                        fontSize = AuralisTypeScale.Caption,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogPodcastRow(
    podcast: CatalogPodcast,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
    ) {
        Row(
            modifier = Modifier.padding(AuralisSpacing.X4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(56.dp)
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
                    fontSize = AuralisTypeScale.BodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = podcast.description,
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.Caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (podcast.isAiGenerated) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI Generated · ${podcast.episodeCount} اپیزود",
                        color = AuralisColors.AccentCyan,
                        fontSize = AuralisTypeScale.Overline,
                    )
                }
            }
        }
    }
}
