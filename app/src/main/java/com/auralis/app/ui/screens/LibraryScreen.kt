package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.data.Episode
import com.auralis.app.ui.components.AuralisEmptyState
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

@Composable
fun LibraryScreen(
    episodes: List<Episode>,
    onEpisodeClick: (Episode) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            FilledTonalButton(
                onClick = onImportClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AuralisColors.SurfaceElevated,
                    contentColor = AuralisColors.TextPrimary,
                ),
                shape = RoundedCornerShape(AuralisCorners.Button),
            ) {
                Text("افزودن فایل", fontSize = AuralisTypeScale.Caption)
            }
        }

        if (episodes.isEmpty()) {
            Box(Modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AuralisEmptyState(
                    title = "هنوز چیزی ذخیره نکرده‌اید",
                    subtitle = "فایل‌های صوتی پادکست‌های AI را از حافظه گوشی اضافه کنید و آفلاین گوش دهید.",
                    actionLabel = "افزودن فایل صوتی",
                    onAction = onImportClick,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                items(episodes, key = { it.id }) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: Episode,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AuralisCorners.Card))
            .clickable(onClick = onClick),
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
                            listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("A", color = AuralisColors.TextPrimary, fontWeight = FontWeight.Bold)
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
                Spacer(Modifier.height(4.dp))
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
                }
            }
        }
    }
}
