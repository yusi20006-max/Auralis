package com.auralis.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.auralis.app.data.Episode
import com.auralis.app.ui.components.AuralisEmptyState
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

private enum class LibraryFilter {
    All,
    Downloaded,
    LocalFiles,
}

@Composable
fun LibraryScreen(
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
    onEpisodeDelete: (Episode) -> Unit,
    onImportClick: () -> Unit,
    onImportFolderClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<Episode?>(null) }
    var filter by remember { mutableStateOf(LibraryFilter.All) }

    val downloaded = remember(episodes) {
        episodes.filter { it.remoteId != null && it.localUri != null }
    }
    val localOnly = remember(episodes) {
        episodes.filter { it.remoteId == null && it.localUri != null }
    }
    val restWhenAll = remember(episodes, downloaded) {
        val dlIds = downloaded.map { it.id }.toSet()
        episodes.filter { it.id !in dlIds }
    }

    val isEmpty = when (filter) {
        LibraryFilter.All -> episodes.isEmpty()
        LibraryFilter.Downloaded -> downloaded.isEmpty()
        LibraryFilter.LocalFiles -> localOnly.isEmpty()
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
                .padding(vertical = AuralisSpacing.X4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "کتابخانه",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleLargeMax,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2)) {
                FilledTonalButton(
                    onClick = onImportClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AuralisColors.SurfaceElevated,
                        contentColor = AuralisColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(AuralisCorners.Button),
                    modifier = Modifier.semantics { contentDescription = "افزودن فایل صوتی از حافظه" },
                ) {
                    Text("افزودن فایل", fontSize = AuralisTypeScale.Caption)
                }
                FilledTonalButton(
                    onClick = onImportFolderClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AuralisColors.SurfaceElevated,
                        contentColor = AuralisColors.TextPrimary,
                    ),
                    shape = RoundedCornerShape(AuralisCorners.Button),
                    modifier = Modifier.semantics { contentDescription = "افزودن پوشه صوتی از حافظه" },
                ) {
                    Text("افزودن پوشه", fontSize = AuralisTypeScale.Caption)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics { contentDescription = "فیلتر کتابخانه" },
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
        ) {
            LibraryFilterChip(
                label = "همه (${episodes.size})",
                selected = filter == LibraryFilter.All,
                onClick = { filter = LibraryFilter.All },
            )
            LibraryFilterChip(
                label = "دانلود شده (${downloaded.size})",
                selected = filter == LibraryFilter.Downloaded,
                onClick = { filter = LibraryFilter.Downloaded },
            )
            LibraryFilterChip(
                label = "محلی (${localOnly.size})",
                selected = filter == LibraryFilter.LocalFiles,
                onClick = { filter = LibraryFilter.LocalFiles },
            )
        }

        Spacer(modifier = Modifier.height(AuralisSpacing.X3))

        if (isEmpty) {
            val (title, subtitle, action) = when (filter) {
                LibraryFilter.Downloaded -> Triple(
                    "هنوز دانلود خصوصی ندارید",
                    "از بخش کشف یک اپیزود را با «دانلود خصوصی» ذخیره کنید. فایل در مدیریت فایل گوشی دیده نمی‌شود.",
                    null,
                )
                LibraryFilter.LocalFiles -> Triple(
                    "فایل محلی نیست",
                    "از حافظه گوشی فایل یا پوشه صوتی اضافه کنید.",
                    "افزودن فایل صوتی",
                )
                LibraryFilter.All -> Triple(
                    "هنوز چیزی ذخیره نکرده‌اید",
                    "فایل محلی اضافه کنید یا از کشف دانلود خصوصی بگیرید.",
                    "افزودن فایل صوتی",
                )
            }
            AuralisEmptyState(
                title = title,
                subtitle = subtitle,
                actionLabel = action,
                onAction = if (action != null) onImportClick else null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                when (filter) {
                    LibraryFilter.Downloaded -> {
                        items(downloaded, key = { it.id }) { episode ->
                            EpisodeListItem(
                                episode = episode,
                                badge = "آفلاین",
                                onClick = { onEpisodeClick(episode) },
                                onLongClick = { pendingDelete = episode },
                            )
                        }
                    }
                    LibraryFilter.LocalFiles -> {
                        items(localOnly, key = { it.id }) { episode ->
                            EpisodeListItem(
                                episode = episode,
                                badge = null,
                                onClick = { onEpisodeClick(episode) },
                                onLongClick = { pendingDelete = episode },
                            )
                        }
                    }
                    LibraryFilter.All -> {
                        if (downloaded.isNotEmpty()) {
                            item {
                                Text(
                                    text = "دانلود شده‌ها",
                                    color = AuralisColors.TextSecondary,
                                    fontSize = AuralisTypeScale.Caption,
                                    modifier = Modifier.padding(vertical = AuralisSpacing.X2),
                                )
                            }
                            items(downloaded, key = { "dl-${it.id}" }) { episode ->
                                EpisodeListItem(
                                    episode = episode,
                                    badge = "آفلاین",
                                    onClick = { onEpisodeClick(episode) },
                                    onLongClick = { pendingDelete = episode },
                                )
                            }
                        }
                        if (restWhenAll.isNotEmpty()) {
                            item {
                                Text(
                                    text = "سایر",
                                    color = AuralisColors.TextSecondary,
                                    fontSize = AuralisTypeScale.Caption,
                                    modifier = Modifier.padding(vertical = AuralisSpacing.X2),
                                )
                            }
                            items(restWhenAll, key = { "rest-${it.id}" }) { episode ->
                                EpisodeListItem(
                                    episode = episode,
                                    badge = null,
                                    onClick = { onEpisodeClick(episode) },
                                    onLongClick = { pendingDelete = episode },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف از کتابخانه؟") },
            text = {
                Text("عضویت «${target.title}» حذف می‌شود. فایل دانلود خصوصی در صورت وجود پاک نمی‌شود.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEpisodeDelete(target)
                        pendingDelete = null
                    },
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("انصراف") }
            },
        )
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AuralisColors.PrimaryBlue,
            selectedLabelColor = AuralisColors.TextPrimary,
            containerColor = AuralisColors.Surface,
            labelColor = AuralisColors.TextSecondary,
        ),
        shape = RoundedCornerShape(AuralisCorners.Button),
        modifier = Modifier.semantics {
            stateDescription = if (selected) "انتخاب‌شده" else "انتخاب‌نشده"
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeListItem(
    episode: Episode,
    badge: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics {
                contentDescription = buildString {
                    append(episode.title)
                    if (badge != null) {
                        append(". ")
                        append(badge)
                    }
                    append(". نگه داشتن برای حذف")
                }
                if (badge != null) {
                    stateDescription = badge
                }
            },
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
    ) {
        Row(
            modifier = Modifier.padding(AuralisSpacing.X4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(AuralisCorners.Artwork))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet),
                        ),
                    )
                    .semantics { contentDescription = "جلد ${episode.title}" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = episode.title.trim().take(1).ifBlank { "A" },
                    color = AuralisColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                val art = episode.artworkUri
                if (art != null) {
                    AsyncImage(
                        model = art,
                        contentDescription = "تصویر جلد ${episode.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = AuralisColors.TextPrimary,
                    fontSize = AuralisTypeScale.BodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = episode.podcastName,
                        color = AuralisColors.TextSecondary,
                        fontSize = AuralisTypeScale.Caption,
                    )
                    if (episode.isAiGenerated) {
                        Text(
                            text = "AI",
                            color = AuralisColors.AccentCyan,
                            fontSize = AuralisTypeScale.Caption,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (badge != null) {
                        Text(
                            text = badge,
                            color = AuralisColors.PrimaryBlue,
                            fontSize = AuralisTypeScale.Caption,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = "نگه داشتن برای حذف",
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Overline,
                )
            }
        }
    }
}
