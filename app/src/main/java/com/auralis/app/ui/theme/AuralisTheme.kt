package com.auralis.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AuralisDarkColorScheme = darkColorScheme(
    primary = AuralisColors.PrimaryBlue,
    onPrimary = AuralisColors.TextPrimary,
    secondary = AuralisColors.PrimaryViolet,
    onSecondary = AuralisColors.TextPrimary,
    tertiary = AuralisColors.AccentCyan,
    onTertiary = AuralisColors.BackgroundDeep,
    background = AuralisColors.BackgroundDeep,
    onBackground = AuralisColors.TextPrimary,
    surface = AuralisColors.Surface,
    onSurface = AuralisColors.TextPrimary,
    surfaceVariant = AuralisColors.SurfaceElevated,
    onSurfaceVariant = AuralisColors.TextSecondary,
    error = AuralisColors.Error,
    onError = AuralisColors.BackgroundDeep,
    outline = AuralisColors.BorderSubtle,
)

private val AuralisTypography = Typography(
    displayLarge = TextStyle(
        fontSize = AuralisTypeScale.HeroMax,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
    ),
    headlineLarge = TextStyle(
        fontSize = AuralisTypeScale.TitleLargeMax,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontSize = AuralisTypeScale.TitleMediumMax,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontSize = AuralisTypeScale.BodyLarge,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = AuralisTypeScale.BodyLarge,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = AuralisTypeScale.BodyMedium,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontSize = AuralisTypeScale.Caption,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
)

@Composable
fun AuralisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuralisDarkColorScheme,
        typography = AuralisTypography,
        shapes = AuralisShapes,
        content = content,
    )
}
