package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkTheme by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisColors.BackgroundDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AuralisSpacing.X4),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AuralisSpacing.X4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("بازگشت", color = AuralisColors.TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "تنظیمات",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMax,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(64.dp))
        }

        SectionTitle("پخش")
        SettingsCard {
            Column(Modifier.padding(AuralisSpacing.X4)) {
                Text(
                    text = "سرعت پخش",
                    color = AuralisColors.TextPrimary,
                    fontSize = AuralisTypeScale.BodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${\"%.1f\".format(playbackSpeed)}x",
                    color = AuralisColors.PrimaryBlue,
                    fontSize = AuralisTypeScale.Caption,
                )
                Slider(
                    value = playbackSpeed,
                    onValueChange = { playbackSpeed = it },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = AuralisColors.PrimaryBlue,
                        activeTrackColor = AuralisColors.PrimaryViolet,
                        inactiveTrackColor = AuralisColors.SurfaceElevated,
                    ),
                )
            }
        }

        SectionTitle("اعلان‌ها")
        SettingsCard {
            SettingsToggleRow(
                title = "اعلان‌های پخش",
                subtitle = "کنترل از نوتیفیکیشن و قفل صفحه",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it },
            )
        }

        SectionTitle("ظاهر")
        SettingsCard {
            SettingsToggleRow(
                title = "تم تیره",
                subtitle = "Auralis فقط تم تیره پشتیبانی می‌کند",
                checked = darkTheme,
                onCheckedChange = { /* locked to dark */ },
            )
        }

        SectionTitle("درباره")
        SettingsCard {
            Column(Modifier.padding(AuralisSpacing.X4)) {
                Text("Auralis", color = AuralisColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("نسخه ۰.۱.۰", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.Caption)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "پخش‌کننده آفلاین پادکست‌های هوش مصنوعی",
                    color = AuralisColors.TextTertiary,
                    fontSize = AuralisTypeScale.Caption,
                )
            }
        }

        Spacer(Modifier.height(AuralisSpacing.X8))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = AuralisColors.TextSecondary,
        fontSize = AuralisTypeScale.Caption,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = AuralisSpacing.X4, bottom = AuralisSpacing.X2),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        color = AuralisColors.Surface,
        shape = RoundedCornerShape(AuralisCorners.Card),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AuralisSpacing.X4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AuralisColors.TextPrimary,
                checkedTrackColor = AuralisColors.PrimaryBlue,
                uncheckedThumbColor = AuralisColors.TextSecondary,
                uncheckedTrackColor = AuralisColors.SurfaceElevated,
            ),
        )
    }
}
