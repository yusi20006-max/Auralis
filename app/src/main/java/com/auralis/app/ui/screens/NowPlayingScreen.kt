package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.data.Episode
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

@Composable
fun NowPlayingScreen(
    episode: Episode?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisColors.BackgroundDeep)
            .padding(horizontal = AuralisSpacing.X6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AuralisSpacing.X4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) {
                Text("بستن", color = AuralisColors.TextSecondary)
            }
            Text(
                text = "در حال پخش",
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.Caption,
            )
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(AuralisSpacing.X6))

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(AuralisCorners.Large))
                .background(
                    Brush.linearGradient(
                        listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.HeroMax * 2,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(AuralisSpacing.X6))

        Text(
            text = episode?.title ?: "هیچ اپیزودی انتخاب نشده",
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.TitleMediumMax,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(AuralisSpacing.X2))
        Text(
            text = episode?.podcastName ?: "Auralis",
            color = AuralisColors.TextSecondary,
            fontSize = AuralisTypeScale.BodyMedium,
        )
        if (episode?.isAiGenerated == true) {
            Spacer(Modifier.height(AuralisSpacing.X2))
            Surface(
                color = AuralisColors.AccentCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(AuralisCorners.Small),
            ) {
                Text(
                    text = "AI Generated",
                    color = AuralisColors.AccentCyan,
                    fontSize = AuralisTypeScale.Caption,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(AuralisSpacing.X8))

        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = { fraction ->
                onSeek((fraction * durationMs).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = AuralisColors.PrimaryBlue,
                activeTrackColor = AuralisColors.PrimaryViolet,
                inactiveTrackColor = AuralisColors.SurfaceElevated,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(positionMs), color = AuralisColors.TextTertiary, fontSize = AuralisTypeScale.Caption)
            Text(formatTime(durationMs), color = AuralisColors.TextTertiary, fontSize = AuralisTypeScale.Caption)
        }

        Spacer(Modifier.height(AuralisSpacing.X6))

        Row(
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onSeek((positionMs - 15_000).coerceAtLeast(0)) }) {
                Text("-۱۵", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.BodyMedium)
            }

            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(AuralisSpacing.NowPlayingControlMax),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AuralisColors.PrimaryBlue,
                    contentColor = AuralisColors.BackgroundDeep,
                ),
                shape = CircleShape,
            ) {
                Text(
                    text = if (isPlaying) "❚❚" else "▶",
                    fontSize = AuralisTypeScale.TitleMediumMax,
                )
            }

            TextButton(onClick = { onSeek((positionMs + 15_000).coerceAtMost(durationMs)) }) {
                Text("+۱۵", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.BodyMedium)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
