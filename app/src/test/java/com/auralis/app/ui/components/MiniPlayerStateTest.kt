package com.auralis.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerStateTest {

    private fun resolve(
        hasMediaItem: Boolean = true,
        hasPlayerError: Boolean = false,
        isBuffering: Boolean = false,
        hasEnded: Boolean = false,
        isPlaying: Boolean = false,
    ) = resolveMiniPlayerUiState(
        hasMediaItem = hasMediaItem,
        hasPlayerError = hasPlayerError,
        isBuffering = isBuffering,
        hasEnded = hasEnded,
        isPlaying = isPlaying,
    )

    @Test
    fun noMediaItemMeansTheBarIsNotShown() {
        assertEquals(MiniPlayerUiState.NONE, resolve(hasMediaItem = false))
        // Even a leftover "playing" signal must not render a bar with nothing behind it.
        assertEquals(MiniPlayerUiState.NONE, resolve(hasMediaItem = false, isPlaying = true))
        assertEquals(MiniPlayerUiState.NONE, resolve(hasMediaItem = false, hasPlayerError = true))
    }

    @Test
    fun playingIsNeverReportedOptimistically() {
        // Session has media, is ready, but is not actually playing -> must read as paused.
        assertEquals(MiniPlayerUiState.PAUSED, resolve(hasMediaItem = true, isPlaying = false))
        // Buffering without an observed isPlaying must not be dressed up as playing either.
        assertEquals(MiniPlayerUiState.BUFFERING, resolve(isBuffering = true, isPlaying = false))
    }

    @Test
    fun playingIsReportedOnlyWhenTheSessionSaysSo() {
        assertEquals(MiniPlayerUiState.PLAYING, resolve(isPlaying = true))
    }

    @Test
    fun errorTakesPrecedenceOverEveryOtherObservedState() {
        assertEquals(MiniPlayerUiState.ERROR, resolve(hasPlayerError = true, isBuffering = true))
        assertEquals(MiniPlayerUiState.ERROR, resolve(hasPlayerError = true, hasEnded = true))
        assertEquals(MiniPlayerUiState.ERROR, resolve(hasPlayerError = true, isPlaying = true))
    }

    @Test
    fun bufferingIsReportedWhileLoading() {
        assertEquals(MiniPlayerUiState.BUFFERING, resolve(isBuffering = true))
    }

    @Test
    fun endOfFileIsReportedBeforeTheStalePlayingFlag() {
        assertEquals(MiniPlayerUiState.ENDED, resolve(hasEnded = true, isPlaying = true))
        assertEquals(MiniPlayerUiState.ENDED, resolve(hasEnded = true))
    }

    @Test
    fun everyRenderedStateCarriesALabel() {
        val rendered = MiniPlayerUiState.entries.filter { it != MiniPlayerUiState.NONE }
        assertEquals(5, rendered.size)
        rendered.forEach { state ->
            assertTrue("missing label for $state", miniPlayerStateLabel(state).isNotBlank())
        }
    }

    @Test
    fun hiddenStateHasNoLabel() {
        assertEquals("", miniPlayerStateLabel(MiniPlayerUiState.NONE))
    }

    @Test
    fun labelsDistinguishTheStatesTheIssueRequires() {
        assertEquals("در حال پخش", miniPlayerStateLabel(MiniPlayerUiState.PLAYING))
        assertEquals("پخش متوقف است", miniPlayerStateLabel(MiniPlayerUiState.PAUSED))
        assertEquals("در حال بارگذاری صدا…", miniPlayerStateLabel(MiniPlayerUiState.BUFFERING))
        assertEquals("خطای پخش", miniPlayerStateLabel(MiniPlayerUiState.ERROR))
        assertEquals("پخش به پایان رسید", miniPlayerStateLabel(MiniPlayerUiState.ENDED))
    }

    @Test
    fun titlePrefersRealPlaybackMetadata() {
        assertEquals(
            "عنوان واقعی از متادیتا",
            resolveMiniPlayerTitle(
                hasMediaItem = true,
                mediaMetadataTitle = "عنوان واقعی از متادیتا",
                pickedFileName = "scan-01.mp3",
            ),
        )
    }

    @Test
    fun titleFallsBackToThePickedFileNameWhenMetadataIsMissing() {
        assertEquals(
            "scan-01.mp3",
            resolveMiniPlayerTitle(
                hasMediaItem = true,
                mediaMetadataTitle = null,
                pickedFileName = "scan-01.mp3",
            ),
        )
        assertEquals(
            "scan-01.mp3",
            resolveMiniPlayerTitle(
                hasMediaItem = true,
                mediaMetadataTitle = "   ",
                pickedFileName = "scan-01.mp3",
            ),
        )
    }

    @Test
    fun titleUsesThePlaceholderWhenNothingIsAvailable() {
        assertEquals(
            MINI_PLAYER_UNKNOWN_TITLE,
            resolveMiniPlayerTitle(hasMediaItem = true, mediaMetadataTitle = null, pickedFileName = null),
        )
        assertEquals(
            MINI_PLAYER_UNKNOWN_TITLE,
            resolveMiniPlayerTitle(hasMediaItem = true, mediaMetadataTitle = "", pickedFileName = ""),
        )
    }

    @Test
    fun titleIsEmptyWhenThereIsNoMedia() {
        // Nothing to show: the caller must not render the bar at all in this case.
        assertEquals(
            "",
            resolveMiniPlayerTitle(hasMediaItem = false, mediaMetadataTitle = "ignored", pickedFileName = "f.mp3"),
        )
    }
}
