package com.auralis.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioImportTest {

    @Test
    fun recognizesCommonAudioMimeAndExtensions() {
        assertTrue(LocalAudioImport.isLikelyAudio("track.mp3", "audio/mpeg"))
        assertTrue(LocalAudioImport.isLikelyAudio("talk.m4a", null))
        assertTrue(LocalAudioImport.isLikelyAudio("voice.OGG", "application/ogg"))
        assertTrue(LocalAudioImport.isLikelyAudio(null, "audio/wav"))
    }

    @Test
    fun rejectsNonAudio() {
        assertFalse(LocalAudioImport.isLikelyAudio("cover.jpg", "image/jpeg"))
        assertFalse(LocalAudioImport.isLikelyAudio("notes.txt", "text/plain"))
        assertFalse(LocalAudioImport.isLikelyAudio("readme", null))
    }
}
