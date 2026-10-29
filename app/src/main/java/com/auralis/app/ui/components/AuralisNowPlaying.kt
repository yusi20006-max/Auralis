package com.auralis.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralis.app.ui.theme.AuralisColors

/** Now Playing renderer. All playback facts and actions come from the existing Media3 controller. */
@Composable
fun AuralisNowPlaying(
    title: String,
    subtitle: String,
    stateLabel: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasError: Boolean,
    hasMedia: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = durationMs.coerceAtLeast(0L)
    val position = positionMs.coerceIn(0L, duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AuralisColors.Surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            AuralisArtworkContainer(
                modifier = Modifier.fillMaxWidth(0.72f).height(230.dp),
                contentDescription = "Artwork for $title",
            ) {
                Text("♫", color = AuralisColors.AccentCyan, fontSize = 64.sp)
            }
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                color = AuralisColors.TextPrimary,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                text = subtitle,
                modifier = Modifier.fillMaxWidth(),
                color = AuralisColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                text = stateLabel,
                color = if (hasError) MaterialTheme.colorScheme.error else AuralisColors.AccentCyan,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { contentDescription = "Playback state: $stateLabel" },
            )
            if (hasMedia && duration > 0L && !hasError) {
                Slider(
                    value = position.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Playback position"
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatPlaybackTime(position), color = AuralisColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text("-${formatPlaybackTime((duration - position).coerceAtLeast(0L))}", color = AuralisColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Text(
                    if (hasError) "پخش این فایل با خطا مواجه شد" else "زمان پخش در دسترس نیست",
                    color = AuralisColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onPlayPause,
                enabled = hasMedia && !hasError && !isBuffering,
                modifier = Modifier.size(80.dp).semantics {
                    contentDescription = if (isPlaying) "Pause playback" else "Play playback"
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuralisColors.PrimaryViolet,
                    contentColor = AuralisColors.BackgroundDeep,
                ),
            ) {
                Text(if (isBuffering) "…" else if (isPlaying) "Ⅱ" else "▶", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
