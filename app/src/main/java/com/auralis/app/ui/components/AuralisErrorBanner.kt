package com.auralis.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

/**
 * Inline error strip for playback / URI failures.
 */
@Composable
fun AuralisErrorBanner(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = message },
        color = AuralisColors.Error.copy(alpha = 0.18f),
        shape = RoundedCornerShape(AuralisCorners.Card),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = AuralisSpacing.X4, vertical = AuralisSpacing.X3),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = AuralisColors.Error,
                fontSize = AuralisTypeScale.BodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = AuralisColors.PrimaryBlue)
                }
            }
        }
    }
}
