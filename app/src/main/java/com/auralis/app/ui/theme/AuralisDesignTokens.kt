package com.auralis.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Source-of-truth design tokens for Auralis.
 * See docs/DESIGN-SYSTEM.md before introducing new visual values.
 */
object AuralisColors {
    val BackgroundDeep = Color(0xFF070A12)
    val Surface = Color(0xFF0F172A)
    val SurfaceElevated = Color(0xFF1E293B)
    val PrimaryBlue = Color(0xFF4F8CFF)
    val PrimaryViolet = Color(0xFF9B5CFF)
    val AccentCyan = Color(0xFF22D3EE)
    val TextPrimary = Color(0xFFE8F1FF)
    val TextSecondary = Color(0xFF94A3B8)
    val TextTertiary = Color(0xFF64748B)
    val Success = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    // Intentional alias: outlines use the same low-emphasis tone as elevated surfaces.
    val BorderSubtle = SurfaceElevated
}

object AuralisSpacing {
    val Base = 4.dp
    val X2 = 8.dp
    val X3 = 12.dp
    val X4 = 16.dp
    val X6 = 24.dp
    val X8 = 32.dp

    val Artwork = 48.dp
    val MiniPlayerMinHeight = 64.dp
    val MiniPlayerMaxHeight = 72.dp
    val NowPlayingControlMin = 72.dp
    val NowPlayingControlMax = 80.dp
    val WaveformMinHeight = 64.dp
    val WaveformMaxHeight = 80.dp
}

object AuralisCorners {
    val Card = 16.dp
    val Button = 14.dp
    val Artwork = 14.dp
    val Small = 12.dp
    val Large = 20.dp
    val ExtraLarge = 24.dp
}

object AuralisMotion {
    const val StandardDurationMillis = 250
    const val MinDurationMillis = 200
    const val MaxDurationMillis = 300
}

object AuralisTypeScale {
    val HeroMin = 28.sp
    val HeroMax = 32.sp
    val TitleLargeMin = 22.sp
    val TitleLargeMax = 24.sp
    val TitleMediumMin = 18.sp
    val TitleMediumMax = 20.sp
    val BodyLarge = 16.sp
    val BodyMedium = 14.sp
    val Caption = 12.sp
    val Overline = 11.sp
}

object AuralisLineHeight {
    val Hero = 40.sp
    val TitleLarge = 32.sp
    val TitleMedium = 28.sp
    val BodyLarge = 24.sp
    val BodyMedium = 20.sp
    val Caption = 16.sp
    val Overline = 14.sp
}

val AuralisShapes = Shapes(
    extraSmall = RoundedCornerShape(AuralisCorners.Small),
    small = RoundedCornerShape(AuralisCorners.Small),
    medium = RoundedCornerShape(AuralisCorners.Card),
    large = RoundedCornerShape(AuralisCorners.Large),
    extraLarge = RoundedCornerShape(AuralisCorners.ExtraLarge),
)
