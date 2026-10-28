package com.auralis.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class AuralisDesignTokensTest {
    @Test
    fun approvedColorPaletteRemainsStable() {
        assertEquals(Color(0xFF070A12), AuralisColors.BackgroundDeep)
        assertEquals(Color(0xFF0F172A), AuralisColors.Surface)
        assertEquals(Color(0xFF1E293B), AuralisColors.SurfaceElevated)
        assertEquals(Color(0xFF4F8CFF), AuralisColors.PrimaryBlue)
        assertEquals(Color(0xFF9B5CFF), AuralisColors.PrimaryViolet)
        assertEquals(Color(0xFF22D3EE), AuralisColors.AccentCyan)
        assertEquals(Color(0xFFE8F1FF), AuralisColors.TextPrimary)
        assertEquals(Color(0xFF94A3B8), AuralisColors.TextSecondary)
        assertEquals(Color(0xFF64748B), AuralisColors.TextTertiary)
        assertEquals(Color(0xFF34D399), AuralisColors.Success)
        assertEquals(Color(0xFFF87171), AuralisColors.Error)
        assertEquals(AuralisColors.SurfaceElevated, AuralisColors.BorderSubtle)
    }

    @Test
    fun spacingAndControlSizesMatchApprovedSpecification() {
        assertEquals(4.dp, AuralisSpacing.Base)
        assertEquals(8.dp, AuralisSpacing.X2)
        assertEquals(12.dp, AuralisSpacing.X3)
        assertEquals(16.dp, AuralisSpacing.X4)
        assertEquals(24.dp, AuralisSpacing.X6)
        assertEquals(32.dp, AuralisSpacing.X8)
        assertEquals(48.dp, AuralisSpacing.Artwork)
        assertEquals(64.dp, AuralisSpacing.MiniPlayerMinHeight)
        assertEquals(72.dp, AuralisSpacing.MiniPlayerMaxHeight)
        assertEquals(72.dp, AuralisSpacing.NowPlayingControlMin)
        assertEquals(80.dp, AuralisSpacing.NowPlayingControlMax)
    }

    @Test
    fun typographyAndMotionStayWithinApprovedScale() {
        assertEquals(28.sp, AuralisTypeScale.HeroMin)
        assertEquals(32.sp, AuralisTypeScale.HeroMax)
        assertEquals(22.sp, AuralisTypeScale.TitleLargeMin)
        assertEquals(24.sp, AuralisTypeScale.TitleLargeMax)
        assertEquals(18.sp, AuralisTypeScale.TitleMediumMin)
        assertEquals(20.sp, AuralisTypeScale.TitleMediumMax)
        assertEquals(250, AuralisMotion.StandardDurationMillis)
        assertEquals(200, AuralisMotion.MinDurationMillis)
        assertEquals(300, AuralisMotion.MaxDurationMillis)
        assertEquals(40.sp, AuralisLineHeight.Hero)
    }

    @Test
    fun cornerTokensFollowApprovedRadii() {
        assertEquals(12.dp, AuralisCorners.Small)
        assertEquals(14.dp, AuralisCorners.Button)
        assertEquals(14.dp, AuralisCorners.Artwork)
        assertEquals(16.dp, AuralisCorners.Card)
        assertEquals(20.dp, AuralisCorners.Large)
        assertEquals(24.dp, AuralisCorners.ExtraLarge)
    }
}
