package com.auralis.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing

/**
 * Shared Auralis UI building blocks. Keep playback decisions in the caller;
 * these components only render the state and invoke supplied callbacks.
 */

@Composable
fun AuralisPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AuralisCorners.Button)
    val activeBrush = Brush.horizontalGradient(
        listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet),
    )
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(if (enabled && !loading) activeBrush else Brush.horizontalGradient(listOf(disabledColor, disabledColor)))
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = AuralisColors.TextPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Box(
                modifier = Modifier.padding(horizontal = AuralisSpacing.X4, vertical = AuralisSpacing.X3),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

@Composable
fun AuralisSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(AuralisCorners.Button),
        color = if (enabled) AuralisColors.Surface else AuralisColors.Surface.copy(alpha = 0.55f),
        contentColor = if (enabled) AuralisColors.TextPrimary else AuralisColors.TextSecondary,
        border = BorderStroke(1.dp, AuralisColors.BorderSubtle),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = AuralisSpacing.X4, vertical = AuralisSpacing.X3),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
fun AuralisIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription; role = Role.Button },
        shape = CircleShape,
        color = if (enabled) Color.Transparent else AuralisColors.Surface.copy(alpha = 0.55f),
        contentColor = if (enabled) AuralisColors.TextSecondary else AuralisColors.TextTertiary,
        border = BorderStroke(1.dp, AuralisColors.BorderSubtle),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun AuralisAiGeneratedLabel(
    modifier: Modifier = Modifier,
    text: String = "AI Generated",
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = AuralisColors.AccentCyan.copy(alpha = 0.14f),
        contentColor = AuralisColors.AccentCyan,
        border = BorderStroke(1.dp, AuralisColors.AccentCyan.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = AuralisSpacing.X3, vertical = 6.dp),
        )
    }
}

@Composable
fun AuralisArtworkContainer(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AuralisCorners.Artwork))
            .background(AuralisColors.SurfaceElevated)
            .then(
                if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun AuralisEpisodeCard(
    title: String,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    artwork: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(AuralisCorners.Card),
        color = AuralisColors.Surface,
        contentColor = AuralisColors.TextPrimary,
        border = BorderStroke(1.dp, AuralisColors.BorderSubtle.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AuralisSpacing.X4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
        ) {
            AuralisArtworkContainer(
                modifier = Modifier.size(AuralisSpacing.Artwork),
                contentDescription = "Episode artwork",
                content = artwork,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!metadata.isNullOrBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuralisColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(0.dp))
                trailingContent()
            }
        }
    }
}

/** Played, buffered, and remaining portions are visualized separately. */
@Composable
fun AuralisProgressIndicator(
    playedFraction: Float,
    modifier: Modifier = Modifier,
    bufferedFraction: Float = playedFraction,
    contentDescription: String = "Playback progress",
) {
    val played = playedFraction.coerceIn(0f, 1f)
    val buffered = bufferedFraction.coerceIn(played, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 4.dp, max = 8.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val strokeWidth = size.height.coerceAtLeast(4.dp.toPx())
        val y = size.height / 2f
        val cap = StrokeCap.Round
        drawLine(
            color = AuralisColors.SurfaceElevated,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = strokeWidth,
            cap = cap,
        )
        if (buffered > 0f) {
            drawLine(
                color = AuralisColors.TextSecondary.copy(alpha = 0.65f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width * buffered, y),
                strokeWidth = strokeWidth,
                cap = cap,
            )
        }
        if (played > 0f) {
            drawLine(
                brush = Brush.horizontalGradient(listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet)),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width * played, y),
                strokeWidth = strokeWidth,
                cap = cap,
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun AuralisComponentsPreview() {
    com.auralis.app.ui.theme.AuralisTheme {
        Column(
            modifier = Modifier.fillMaxWidth().background(AuralisColors.BackgroundDeep).padding(AuralisSpacing.X4),
            verticalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
        ) {
            AuralisPrimaryButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Primary action", color = AuralisColors.TextPrimary)
            }
            AuralisSecondaryButton(onClick = {}) {
                Text("Secondary action")
            }
            AuralisAiGeneratedLabel()
            AuralisEpisodeCard(
                title = "A sample episode title that may wrap to two lines",
                metadata = "12 min · Offline",
                artwork = { Text("♪", color = AuralisColors.TextSecondary) },
                trailingContent = { Text("▶", color = AuralisColors.TextPrimary) },
            )
            AuralisProgressIndicator(playedFraction = 0.42f, bufferedFraction = 0.68f)
        }
    }
}
