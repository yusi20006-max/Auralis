package com.auralis.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.auralis.app.R

/**
 * Vazirmatn via Google Fonts downloadable provider (Persian-first).
 * Falls back to system sans if the provider is unavailable offline.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val vazirmatn = GoogleFont("Vazirmatn")

val AuralisFontFamily = FontFamily(
    Font(googleFont = vazirmatn, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = vazirmatn, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = vazirmatn, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = vazirmatn, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)
