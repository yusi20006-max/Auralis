package com.auralis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisCorners
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTypeScale

private val categories = listOf(
    "همه",
    "علمی تخیلی",
    "فلسفه",
    "روانشناسی",
    "تکنولوژی",
    "داستان",
)

@Composable
fun ExploreScreen(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf("همه") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuralisColors.BackgroundDeep)
            .padding(horizontal = AuralisSpacing.X4),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item {
            Text(
                text = "کشف",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleLargeMax,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = AuralisSpacing.X4),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X2),
            ) {
                categories.forEach { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = AuralisTypeScale.Caption,
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
            Spacer(Modifier.height(AuralisSpacing.X6))
        }

        item {
            Text(
                text = "پیشنهاد برای شما",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMin,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(AuralisSpacing.X3))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AuralisSpacing.X3),
            ) {
                ExploreCard(
                    title = "افق‌های ناشناخته",
                    subtitle = "سفر به دنیاهای فراتر",
                    gradient = listOf(AuralisColors.PrimaryBlue, AuralisColors.PrimaryViolet),
                )
                ExploreCard(
                    title = "ذهن کیهانی",
                    subtitle = "آینده هوش و انسان",
                    gradient = listOf(AuralisColors.PrimaryViolet, AuralisColors.AccentCyan),
                )
            }
            Spacer(Modifier.height(AuralisSpacing.X6))
        }

        item {
            Text(
                text = "شروع با فایل محلی",
                color = AuralisColors.TextPrimary,
                fontSize = AuralisTypeScale.TitleMediumMin,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(AuralisSpacing.X3))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AuralisCorners.Card))
                    .clickable(onClick = onImportClick),
                color = AuralisColors.Surface,
                shape = RoundedCornerShape(AuralisCorners.Card),
            ) {
                Column(
                    modifier = Modifier.padding(AuralisSpacing.X4),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "افزودن پادکست از حافظه",
                        color = AuralisColors.TextPrimary,
                        fontSize = AuralisTypeScale.BodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "فایل‌های صوتی AI خود را وارد کنید و آفلاین گوش دهید",
                        color = AuralisColors.TextSecondary,
                        fontSize = AuralisTypeScale.Caption,
                    )
                }
            }
            Spacer(Modifier.height(AuralisSpacing.X4))
            Text(
                text = "کاتالوگ آنلاین در نسخه‌های بعدی اضافه می‌شود.",
                color = AuralisColors.TextTertiary,
                fontSize = AuralisTypeScale.Caption,
            )
        }
    }
}

@Composable
private fun ExploreCard(
    title: String,
    subtitle: String,
    gradient: List<androidx.compose.ui.graphics.Color>,
) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .height(140.dp),
        shape = RoundedCornerShape(AuralisCorners.Card),
        color = AuralisColors.Surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(AuralisSpacing.X4),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Surface(
                    color = AuralisColors.AccentCyan.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(AuralisCorners.Small),
                ) {
                    Text(
                        text = "AI Generated",
                        color = AuralisColors.AccentCyan,
                        fontSize = AuralisTypeScale.Overline,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    color = AuralisColors.TextPrimary,
                    fontSize = AuralisTypeScale.BodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = AuralisColors.TextSecondary,
                    fontSize = AuralisTypeScale.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
