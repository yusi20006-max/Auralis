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
import com.auralis.app.ui.components.AuralisWaveform
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

private val SPEED_PRESETS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

@Composable
fun NowPlayingScreen(
    episode: Episode?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

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
            // Current speed badge
            Surface(
                color = AuralisColors.SurfaceElevated,
                shape = RoundedCornerShape(AuralisCorners.Small),
            ) {
                Text(
                    text = String.format("%.2gx", playbackSpeed).replace("0x", "x"),
                    color = AuralisColors.PrimaryBlue,
                    fontSize = AuralisTypeScale.Caption,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(AuralisSpacing.X3))

        Box(
            modifier = Modifier
                .size(220.dp)
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

        Spacer(Modifier.height(AuralisSpacing.X3))

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

        Spacer(Modifier.height(AuralisSpacing.X3))

        AuralisWaveform(
            isPlaying = isPlaying,
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(AuralisSpacing.X2))

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

        Spacer(Modifier.height(AuralisSpacing.X4))

        Row(
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X6),
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

            TextButton(onClick = { onSeek((positionMs + 15_000).coerceAtMost(durationMs.coerceAtLeast(0))) }) {
                Text("+۱۵", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.BodyMedium)
            }
        }

        Spacer(Modifier.height(AuralisSpacing.X4))

        Text(
            text = "سرعت پخش",
            color = AuralisColors.TextSecondary,
            fontSize = AuralisTypeScale.Caption,
            modifier = Modifier.padding(bottom = AuralisSpacing.X2),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SPEED_PRESETS.forEach { speed ->
                val selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                FilterChip(
                    selected = selected,
                    onClick = { onSpeedChange(speed) },
                    label = {
                        Text(
                            text = formatSpeedLabel(speed),
                            fontSize = AuralisTypeScale.Caption,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
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
    }
}

private fun formatSpeedLabel(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}x"
    } else {
        String.format("%.2gx", speed)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
