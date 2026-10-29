package com.auralis.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisSpacing
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Decorative waveform for Now Playing.
 * Visual-only: progress is still controlled by the slider / Media3 position.
 */
@Composable
fun AuralisWaveform(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 1600 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val effectivePhase = if (isPlaying) phase else 0f
    val clampedProgress = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(AuralisSpacing.WaveformMinHeight),
    ) {
        val barWidth = size.width / (barCount * 1.8f)
        val gap = barWidth * 0.8f
        val totalBarSpace = barWidth + gap
        val startX = (size.width - (totalBarSpace * barCount - gap)) / 2f
        val midY = size.height / 2f

        for (i in 0 until barCount) {
            val t = i / (barCount - 1f)
            val envelope = 0.35f + 0.65f * abs(sin((t * PI * 2.2).toFloat()))
            val anim = if (isPlaying) {
                0.55f + 0.45f * abs(sin(effectivePhase + t * 6f))
            } else {
                0.45f
            }
            val h = size.height * envelope * anim * 0.9f
            val x = startX + i * totalBarSpace
            val played = t <= clampedProgress
            val color = if (played) {
                Brush.verticalGradient(
                    listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet)
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        AuralisColors.SurfaceElevated,
                        AuralisColors.SurfaceElevated.copy(alpha = 0.7f),
                    )
                )
            }
            drawRoundRect(
                brush = color,
                topLeft = Offset(x, midY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
