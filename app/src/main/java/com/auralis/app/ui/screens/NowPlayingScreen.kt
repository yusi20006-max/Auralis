package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = AuralisSpacing.X6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: fully below status bar, large touch targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AuralisSpacing.X2, bottom = AuralisSpacing.X3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    text = "بستن",
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.BodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = "در حال پخش",
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.Caption,
            )
            // Force LTR so "1x" does not render as "x.1" under RTL
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Surface(
                    color = AuralisColors.SurfaceElevated,
                    shape = RoundedCornerShape(AuralisCorners.Small),
                    modifier = Modifier.height(40.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = formatSpeedLabel(playbackSpeed),
                            color = AuralisColors.PrimaryBlue,
                            fontSize = AuralisTypeScale.Caption,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AuralisSpacing.X2))

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(AuralisCorners.Large))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet),
                    ),
                ),
            contentAlignment = Alignment.Center,
            content = {
                Text(
                    text = "A",
                    color = AuralisColors.TextPrimary,
                    fontSize = AuralisTypeScale.HeroMax * 2,
                    fontWeight = FontWeight.Bold,
                )
            },
        )

        Spacer(modifier = Modifier.height(AuralisSpacing.X3))

        Text(
            text = episode?.title ?: "هیچ اپیزودی انتخاب نشده",
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.TitleMediumMax,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(AuralisSpacing.X2))
        Text(
            text = episode?.podcastName ?: "Auralis",
            color = AuralisColors.TextSecondary,
            fontSize = AuralisTypeScale.BodyMedium,
        )
        if (episode?.isAiGenerated == true) {
            Spacer(modifier = Modifier.height(AuralisSpacing.X2))
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

        Spacer(modifier = Modifier.height(AuralisSpacing.X3))

        AuralisWaveform(
            isPlaying = isPlaying,
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(AuralisSpacing.X2))

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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    formatTime(positionMs),
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Caption,
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    formatTime(durationMs),
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Caption,
                )
            }
        }

        Spacer(modifier = Modifier.height(AuralisSpacing.X4))

        Row(
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onSeek((positionMs - 15_000).coerceAtLeast(0)) },
                modifier = Modifier.height(48.dp),
            ) {
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

            TextButton(
                onClick = { onSeek((positionMs + 15_000).coerceAtMost(durationMs.coerceAtLeast(0))) },
                modifier = Modifier.height(48.dp),
            ) {
                Text("+۱۵", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.BodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(AuralisSpacing.X4))

        Text(
            text = "سرعت پخش",
            color = AuralisColors.TextSecondary,
            fontSize = AuralisTypeScale.Caption,
            modifier = Modifier.padding(bottom = AuralisSpacing.X2),
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
