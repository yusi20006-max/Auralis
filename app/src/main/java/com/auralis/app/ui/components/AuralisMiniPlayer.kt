package com.auralis.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTheme

/**
 * Persistent playback bar bound to the state the caller observed on the existing Media3 session.
 *
 * This composable renders only. It never creates a player, never starts playback on its own, and
 * never guesses: [state] and [showPauseAction] must come from real session values, and the bar is
 * simply not composed when there is no media ([MiniPlayerUiState.NONE]).
 *
 * Layout contract: height stays inside `AuralisSpacing.MiniPlayerMinHeight..MaxHeight` (64–72dp)
 * and artwork is `AuralisSpacing.Artwork` (48dp). Row order flips automatically in RTL.
 */
@Composable
fun AuralisMiniPlayer(
    title: String,
    state: MiniPlayerUiState,
    showPauseAction: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateLabel = miniPlayerStateLabel(state)
    val labelColor = if (state == MiniPlayerUiState.ERROR) {
        AuralisColors.Error
    } else {
        AuralisColors.TextSecondary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AuralisSpacing.MiniPlayerMinHeight, max = AuralisSpacing.MiniPlayerMaxHeight),
        shape = RoundedCornerShape(AuralisCorners.Card),
        color = AuralisColors.SurfaceElevated,
        contentColor = AuralisColors.TextPrimary,
        border = BorderStroke(1.dp, AuralisColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AuralisSpacing.X3, vertical = AuralisSpacing.X2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
        ) {
            AuralisArtworkContainer(
                modifier = Modifier.size(AuralisSpacing.Artwork),
                contentDescription = "جلد تصویری برای این فایل موجود نیست",
            ) {
                Text(
                    text = "♪",
                    color = AuralisColors.TextSecondary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AuralisColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
                ) {
                    if (state == MiniPlayerUiState.BUFFERING) {
                        // Deliberately no contentDescription: the label immediately beside it already
                        // announces the state, so describing the spinner too would make TalkBack say
                        // the same thing twice (the same reason stateDescription is absent here).
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = AuralisColors.AccentCyan,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AuralisIconButton(
                contentDescription = if (showPauseAction) "مکث پخش" else "ادامه پخش",
                onClick = onPlayPause,
            ) {
                PlaybackControlGlyph(showPause = showPauseAction, color = LocalContentColor.current)
            }
        }
    }
}

/**
 * Drawn instead of a font glyph so the control renders identically on every device and adds no
 * icon-library dependency.
 */
@Composable
private fun PlaybackControlGlyph(showPause: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val height = size.height
        if (showPause) {
            val barWidth = size.width * 0.24f
            val gap = size.width * 0.16f
            val start = (size.width - (barWidth * 2 + gap)) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(start, 0f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(start + barWidth + gap, 0f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.26f, 0f)
                lineTo(size.width * 0.84f, height / 2f)
                lineTo(size.width * 0.26f, height)
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070A12, widthDp = 360)
@Composable
private fun AuralisMiniPlayerPreview() {
    AuralisTheme {
        Column(
            modifier = Modifier
                .background(AuralisColors.BackgroundDeep)
                .padding(AuralisSpacing.X4),
            verticalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
        ) {
            AuralisMiniPlayer(
                title = "یک عنوان بسیار طولانی برای بررسی برش‌خوردگی متن در نوار کوچک پخش",
                state = MiniPlayerUiState.PLAYING,
                showPauseAction = true,
                onPlayPause = {},
            )
            AuralisMiniPlayer(
                title = "نمونه-فایل-صوتی.m4a",
                state = MiniPlayerUiState.BUFFERING,
                showPauseAction = true,
                onPlayPause = {},
            )
            AuralisMiniPlayer(
                title = "نمونه-فایل-صوتی.mp3",
                state = MiniPlayerUiState.ERROR,
                showPauseAction = false,
                onPlayPause = {},
            )
        }
    }
}
