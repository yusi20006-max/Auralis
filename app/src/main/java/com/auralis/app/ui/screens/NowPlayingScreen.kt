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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    val title = episode?.title?.takeIf { it.isNotBlank() } ?: "بدون عنوان"
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AuralisSpacing.X2, bottom = AuralisSpacing.X3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "بستن صفحه پخش" },
            ) {
                Text(
                    text = "بستن",
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.BodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = if (isPlaying) "در حال پخش" else "متوقف",
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.Caption,
                modifier = Modifier.semantics {
                    contentDescription = if (isPlaying) "وضعیت: در حال پخش" else "وضعیت: متوقف"
                },
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Surface(
                    color = AuralisColors.SurfaceElevated,
                    shape = RoundedCornerShape(AuralisCorners.Small),
                    modifier = Modifier
                        .height(40.dp)
                        .semantics {
                            contentDescription = "سرعت فعلی ${formatSpeedLabel(playbackSpeed)}"
                        },
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
                )
                .semantics {
                    contentDescription = "جلد اپیزود"
                    stateDescription = title
                },
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

        Spacer(modifier = Modifier.height(AuralisSpacing.X6))

        Text(
            text = title,
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.HeroMax,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!episode?.podcastName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(AuralisSpacing.X2))
            Text(
                text = episode?.podcastName.orEmpty(),
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.BodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(AuralisSpacing.X4))

        AuralisWaveform(
            progress = progress.coerceIn(0f, 1f),
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        )

        Spacer(modifier = Modifier.height(AuralisSpacing.X3))

        Slider(
            value = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
            onValueChange = { fraction ->
                if (durationMs > 0) onSeek((fraction * durationMs).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "موقعیت پخش"
                    stateDescription = if (durationMs > 0) {
                        "${formatTime(positionMs)} از ${formatTime(durationMs)}"
                    } else {
                        "مدت نامشخص"
                    }
                },
            colors = SliderDefaults.colors(
                thumbColor = AuralisColors.PrimaryBlue,
                activeTrackColor = AuralisColors.PrimaryBlue,
                inactiveTrackColor = AuralisColors.SurfaceElevated,
            ),
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
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "۱۵ ثانیه به عقب" },
            ) {
                Text("-۱۵", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.BodyMedium)
            }

            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(AuralisSpacing.NowPlayingControlMax)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isPlaying) "مکث پخش" else "شروع پخش"
                    },
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
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "۱۵ ثانیه به جلو" },
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
                        modifier = Modifier.semantics {
                            contentDescription = "سرعت پخش ${formatSpeedLabel(speed)}"
                            stateDescription = if (selected) "انتخاب‌شده" else "انتخاب‌نشده"
                        },
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
    return if (speed == 1.0f) "1x" else "${speed}x"
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
