package com.auralis.app.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalUriAccessTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun remoteHttpIsAlwaysReadableProbe() {
        val uri = Uri.parse("https://example.com/a.mp3")
        assertTrue(LocalUriAccess.isRemote(uri))
        assertTrue(LocalUriAccess.canRead(context, uri))
    }

    @Test
    fun missingFileUriIsNotReadable() {
        val uri = Uri.fromFile(File("/tmp/auralis-does-not-exist-${System.nanoTime()}.bin"))
        assertFalse(LocalUriAccess.canRead(context, uri))
    }

    @Test
    fun existingReadableFileUriIsReadable() {
        val file = File.createTempFile("auralis-test-", ".bin")
        file.writeBytes(byteArrayOf(1, 2, 3))
        try {
            assertTrue(LocalUriAccess.canRead(context, Uri.fromFile(file)))
        } finally {
            file.delete()
        }
    }

    @Test
    fun resolveFallsBackToAudioUrlWhenLocalMissing() {
        val episode = Episode(
            id = "e1",
            title = "t",
            localUri = Uri.fromFile(File("/tmp/missing-auralis-file")),
            audioUrl = "https://cdn.example/a.mp3",
        )
        val resolved = LocalUriAccess.resolvePlayableUri(context, episode)
        assertTrue(resolved.toString().startsWith("https://"))
    }

    @Test
    fun resolveReturnsNullWhenNoSource() {
        val episode = Episode(id = "e2", title = "t")
        assertNull(LocalUriAccess.resolvePlayableUri(context, episode))
    }
}
