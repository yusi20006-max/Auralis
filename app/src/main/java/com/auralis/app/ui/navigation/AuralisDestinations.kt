package com.auralis.app.ui.navigation

/**
 * Top-level navigation destinations for Auralis.
 */
enum class AuralisDestination(val route: String, val label: String) {
    Library("library", "کتابخانه"),
    Explore("explore", "کشف"),
    Profile("profile", "پروفایل"),
    NowPlaying("now_playing", "در حال پخش"),
    EpisodeDetail("episode_detail", "جزئیات قسمت"),
}
