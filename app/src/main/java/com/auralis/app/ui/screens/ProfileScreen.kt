package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

@Composable
fun ProfileScreen(
    episodeCount: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisColors.BackgroundDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuralisSpacing.X4),
    ) {
        Text(
            text = "پروفایل",
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.TitleLargeMax,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = AuralisSpacing.X4),
        )

        // Avatar + name
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
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
                    fontSize = AuralisTypeScale.HeroMax,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(AuralisSpacing.X3))
            Text(
                text = "شنونده Auralis",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMax,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "حالت آفلاین · بدون حساب کاربری",
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.Caption,
            )
        }

        Spacer(Modifier.height(AuralisSpacing.X6))

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
        ) {
            StatCard(modifier = Modifier.weight(1f), value = "$episodeCount", label = "اپیزود")
            StatCard(modifier = Modifier.weight(1f), value = "—", label = "ساعت گوش‌دادن")
            StatCard(modifier = Modifier.weight(1f), value = "AI", label = "محتوا")
        }

        Spacer(Modifier.height(AuralisSpacing.X6))

        Text(
            text = "کتابخانه من",
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.BodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(AuralisSpacing.X3))

        ProfileRow(title = "علاقه‌مندی‌ها", subtitle = "به زودی")
        ProfileRow(title = "دانلودها", subtitle = "به زودی")
        ProfileRow(title = "تاریخچه گوش دادن", subtitle = "$episodeCount قسمت")

        Spacer(Modifier.height(AuralisSpacing.X6))

        Text(
            text = "تنظیمات",
            color = AuralisColors.TextPrimary,
            fontSize = AuralisTypeScale.BodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(AuralisSpacing.X3))

        ProfileRow(
            title = "تنظیمات پخش و ظاهر",
            subtitle = "سرعت، اعلان، تم",
            onClick = onOpenSettings,
        )

        Spacer(Modifier.height(AuralisSpacing.X8))

        Text(
            text = "Auralis · نسخه ۰.۱.۰",
            color = AuralisColors.TextTertiary,
            fontSize = AuralisTypeScale.Caption,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(AuralisSpacing.X8))
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
    ) {
        Column(
            modifier = Modifier.padding(AuralisSpacing.X4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMax,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = AuralisColors.TextSecondary,
                fontSize = AuralisTypeScale.Caption,
            )
        }
    }
}

@Composable
private fun ProfileRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AuralisSpacing.X2)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AuralisSpacing.X4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = title,
                    color = AuralisColors.TextPrimary,
                    fontSize = AuralisTypeScale.BodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.Caption,
                )
            }
            if (onClick != null) {
                Text("›", color = AuralisColors.TextTertiary, fontSize = AuralisTypeScale.TitleMediumMax)
            }
        }
    }
}
