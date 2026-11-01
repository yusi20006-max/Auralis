package com.auralis.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.data.download.DownloadIdentity
import com.auralis.app.data.catalog.CatalogLoadState
import com.auralis.app.data.catalog.CatalogEpisode
import com.auralis.app.data.catalog.CatalogPodcast
import com.auralis.app.data.catalog.CatalogRepository
import com.auralis.app.ui.components.AuralisErrorBanner
import coil.compose.AsyncImage
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
    onCancelDownload: (CatalogPodcast, CatalogEpisode) -> Unit = { _, _ -> },
    downloadedIdentities: Set<DownloadIdentity> = emptySet(),
    downloadingIdentities: Set<DownloadIdentity> = emptySet(),
    downloadProgress: Map<DownloadIdentity, Float> = emptyMap(),
    downloadErrors: Map<DownloadIdentity, String> = emptyMap(),
    libraryIdentities: Set<DownloadIdentity> = emptySet(),
    isDownloaded: (DownloadIdentity) -> Boolean = { downloadedIdentities.contains(it) },
    onAddToLibrary: suspend (CatalogPodcast, CatalogEpisode) -> Unit = { _, _ -> },
    onRemoveFromLibrary: suspend (CatalogPodcast, CatalogEpisode) -> Unit = { _, _ -> },
    catalogRepository: CatalogRepository = remember { CatalogRepository() },
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf("همه") }
    var selectedPodcast by remember { mutableStateOf<CatalogPodcast?>(null) }
    val podcasts by catalogRepository.observePodcasts(selectedCategory)
        .collectAsState(initial = emptyList())
    val catalogLoadState by catalogRepository.loadState.collectAsState()
    val scope = rememberCoroutineScope()
    val retryCatalog = {
        scope.launch { catalogRepository.refresh() }
    }

    // Sample/last-known-good content stays visible while remote refresh runs.
    LaunchedEffect(catalogRepository) {
        catalogRepository.refresh()
    }

    val podcast = selectedPodcast
    if (podcast != null) {
        BackHandler { selectedPodcast = null }
        CatalogPodcastDetailScreen(
            podcast = podcast,
            catalogRepository = catalogRepository,
            downloadedIdentities = downloadedIdentities,
            downloadingIdentities = downloadingIdentities,
            downloadProgress = downloadProgress,
            downloadErrors = downloadErrors,
            libraryIdentities = libraryIdentities,
            isDownloaded = isDownloaded,
            loadState = catalogLoadState,
            onRetryCatalog = retryCatalog,
            onAddToLibrary = { episode -> onAddToLibrary(podcast, episode) },
            onRemoveFromLibrary = { episode -> onRemoveFromLibrary(podcast, episode) },
            onBack = { selectedPodcast = null },
            onPlayEpisode = { episode -> onStreamEpisode(podcast, episode) },
            onDownloadEpisode = { episode -> onDownloadEpisode(podcast, episode) },
            onCancelDownload = { episode -> onCancelDownload(podcast, episode) },
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
                text = "کاوش",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleLargeMax,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = AuralisSpacing.X4),
            )
            Text(
                text = "پادکست‌های AI · استریم یا دانلود",
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.Caption,
                modifier = Modifier.padding(bottom = AuralisSpacing.X4),
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
                    val selected = selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuralisColors.PrimaryBlue,
                            selectedLabelColor = AuralisColors.TextPrimary,
                            containerColor = AuralisColors.Surface,
                            labelColor = AuralisColors.TextSecondary,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(AuralisSpacing.X4))
            CatalogLoadStatus(
                loadState = catalogLoadState,
                onRetry = retryCatalog,
            )
        }

        if (podcasts.isEmpty()) {
            item {
                Text(
                    text = if (selectedCategory == "همه") {
                        "فعلاً پادکستی برای نمایش وجود ندارد."
                    } else {
                        "در این دسته‌بندی پادکستی پیدا نشد."
                    },
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.BodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AuralisSpacing.X8),
                )
                if (selectedCategory != "همه") {
                    TextButton(onClick = { selectedCategory = "همه" }) {
                        Text("نمایش همه دسته‌ها")
                    }
                }
            }
        } else items(podcasts, key = { it.id }) { item ->
            PodcastCard(
                podcast = item,
                onClick = { selectedPodcast = item },
            )
            Spacer(modifier = Modifier.height(AuralisSpacing.X3))
        }

        item {
            Spacer(modifier = Modifier.height(AuralisSpacing.X4))
            Surface(
                onClick = onImportClick,
                color = AuralisColors.Surface,
                shape = RoundedCornerShape(AuralisCorners.Card),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(AuralisSpacing.X4)) {
                    Text(
                        text = "وارد کردن فایل صوتی",
                        color = AuralisColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "از حافظه دستگاه",
                        color = AuralisColors.TextSecondary,
                        fontSize = AuralisTypeScale.Caption,
                    )
                }
            }
            Spacer(modifier = Modifier.height(AuralisSpacing.X8))
        }
    }
}

@Composable
private fun PodcastCard(
    podcast: CatalogPodcast,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(AuralisSpacing.X4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PodcastArtwork(
                coverUrl = podcast.coverUrl,
                title = podcast.title,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.width(AuralisSpacing.X3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    color = AuralisColors.TextPrimary,
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
                Text(
                    text = "${podcast.episodeCount} قسمت · ${podcast.category}",
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Caption,
                )
            }
        }
    }
}


@Composable
internal fun CatalogLoadStatus(
    loadState: CatalogLoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (loadState) {
        CatalogLoadState.Idle,
        is CatalogLoadState.Success -> Unit
        CatalogLoadState.Loading -> {
            Column(modifier = modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AuralisColors.PrimaryBlue,
                    trackColor = AuralisColors.SurfaceElevated,
                )
                Text(
                    text = "در حال بررسی به‌روزرسانی کاتالوگ…",
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Caption,
                    modifier = Modifier.padding(top = AuralisSpacing.X2),
                )
            }
        }
        is CatalogLoadState.Error -> {
            AuralisErrorBanner(
                message = "دریافت کاتالوگ آنلاین ناموفق بود؛ محتوای قبلی حفظ شده است.",
                actionLabel = "تلاش دوباره",
                onAction = onRetry,
                modifier = modifier,
            )
        }
        is CatalogLoadState.Skipped -> {
            Text(
                text = "دریافت آنلاین غیرفعال است؛ محتوای موجود نمایش داده می‌شود.",
                color = AuralisColors.TextTertiary,
                fontSize = AuralisTypeScale.Caption,
                modifier = modifier.padding(vertical = AuralisSpacing.X2),
            )
        }
    }
}

@Composable
internal fun PodcastArtwork(
    coverUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AuralisCorners.Artwork))
            .background(
                Brush.linearGradient(
                    listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet),
                ),
            )
            .semantics { contentDescription = "جلد پادکست $title" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.trim().take(1).ifBlank { "A" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "تصویر جلد $title",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
