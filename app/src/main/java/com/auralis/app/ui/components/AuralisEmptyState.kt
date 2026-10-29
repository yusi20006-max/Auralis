package com.auralis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

/**
 * Shared empty-state layout used across Library / Explore / Search.
 */
@Composable
fun AuralisEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AuralisSpacing.X6, vertical = AuralisSpacing.X8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            AuralisColors.PrimaryBlue.copy(alpha = 0.35f),
                            AuralisColors.PrimaryViolet.copy(alpha = 0.35f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AuralisCorners.Artwork))
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
                    fontWeight = FontWeight.Bold,
                    fontSize = AuralisTypeScale.TitleMediumMax,
                )
            }
        }

        Spacer(Modifier.height(AuralisSpacing.X6))

        Text(
            text = title,
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.TitleMediumMax,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AuralisSpacing.X2))
        Text(
            text = subtitle,
            color = AuralisColors.TextSecondary,
            fontSize = AuralisTypeScale.BodyMedium,
            textAlign = TextAlign.Center,
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(AuralisSpacing.X6))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuralisColors.PrimaryBlue,
                    contentColor = AuralisColors.BackgroundDeep,
                ),
                shape = RoundedCornerShape(AuralisCorners.Button),
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
