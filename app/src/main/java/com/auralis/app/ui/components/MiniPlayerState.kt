package com.auralis.app.ui.components

/**
 * States the mini player is allowed to render.
 * [NONE] means no media is loaded, so the bar is not shown at all.
 */
enum class MiniPlayerUiState {
    NONE,
    PLAYING,
    PAUSED,
    BUFFERING,
    ENDED,
    ERROR,
}

/**
 * Maps raw player facts to the one state the mini player may display.
 *
 * Deliberately pure and Media3-free so it can be unit tested on the JVM. Every input is an
 * observed player fact, never an optimistic UI flag: "playing" is only ever reported when the
 * caller actually observed `isPlaying` from the session.
 *
 * Precedence: nothing loaded -> error -> buffering -> ended -> playing -> paused.
 */
fun resolveMiniPlayerUiState(
    hasMediaItem: Boolean,
    hasPlayerError: Boolean,
    isBuffering: Boolean,
    hasEnded: Boolean,
    isPlaying: Boolean,
): MiniPlayerUiState = when {
    !hasMediaItem -> MiniPlayerUiState.NONE
    hasPlayerError -> MiniPlayerUiState.ERROR
    isBuffering -> MiniPlayerUiState.BUFFERING
    hasEnded -> MiniPlayerUiState.ENDED
    isPlaying -> MiniPlayerUiState.PLAYING
    else -> MiniPlayerUiState.PAUSED
}

/** User-facing state label. Empty only for [MiniPlayerUiState.NONE], which is never rendered. */
fun miniPlayerStateLabel(state: MiniPlayerUiState): String = when (state) {
    MiniPlayerUiState.NONE -> ""
    MiniPlayerUiState.PLAYING -> "در حال پخش"
    MiniPlayerUiState.PAUSED -> "پخش متوقف است"
    MiniPlayerUiState.BUFFERING -> "در حال بارگذاری صدا…"
    MiniPlayerUiState.ENDED -> "پخش به پایان رسید"
    MiniPlayerUiState.ERROR -> "خطای پخش"
}

/** Placeholder used when a track is loaded but neither its metadata nor its file name is usable. */
const val MINI_PLAYER_UNKNOWN_TITLE = "عنوانی در دسترس نیست"

/**
 * Title shown on the bar: real playback metadata first, then the real picked file name, then the
 * placeholder. Empty only when no media is loaded — that case is never rendered.
 *
 * [pickedFileName] must already be the genuine file name, not the screen's "nothing selected"
 * sentinel; callers filter that out before passing it in.
 */
fun resolveMiniPlayerTitle(
    hasMediaItem: Boolean,
    mediaMetadataTitle: String?,
    pickedFileName: String?,
): String = when {
    !hasMediaItem -> ""
    !mediaMetadataTitle.isNullOrBlank() -> mediaMetadataTitle
    !pickedFileName.isNullOrBlank() -> pickedFileName
    else -> MINI_PLAYER_UNKNOWN_TITLE
}
