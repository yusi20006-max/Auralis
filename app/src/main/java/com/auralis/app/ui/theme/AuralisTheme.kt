package com.auralis.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection

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
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.HeroMax,
        fontWeight = FontWeight.Bold,
        lineHeight = AuralisLineHeight.Hero,
    ),
    headlineLarge = TextStyle(
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.TitleLargeMax,
        fontWeight = FontWeight.SemiBold,
        lineHeight = AuralisLineHeight.TitleLarge,
    ),
    titleLarge = TextStyle(
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.TitleMediumMax,
        fontWeight = FontWeight.SemiBold,
        lineHeight = AuralisLineHeight.TitleMedium,
    ),
    titleMedium = TextStyle(
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.BodyLarge,
        fontWeight = FontWeight.SemiBold,
        lineHeight = AuralisLineHeight.BodyLarge,
    ),
    bodyLarge = TextStyle(
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.BodyLarge,
        fontWeight = FontWeight.Normal,
        lineHeight = AuralisLineHeight.BodyLarge,
    ),
    bodyMedium = TextStyle(
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.BodyMedium,
        fontWeight = FontWeight.Normal,
        lineHeight = AuralisLineHeight.BodyMedium,
    ),
    labelSmall = TextStyle(
        fontFamily = AuralisFontFamily,
        fontSize = AuralisTypeScale.Caption,
        fontWeight = FontWeight.Normal,
        lineHeight = AuralisLineHeight.Caption,
    ),
)

/**
 * Auralis theme: always dark + forced RTL for Persian UI + Vazirmatn.
 */
@Composable
fun AuralisTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = AuralisDarkColorScheme,
            typography = AuralisTypography,
            shapes = AuralisShapes,
            content = content,
        )
    }
}
