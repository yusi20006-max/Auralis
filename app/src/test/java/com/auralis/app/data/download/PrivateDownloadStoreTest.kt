package com.auralis.app.data.download

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivateDownloadStoreTest {

    private lateinit var store: PrivateDownloadStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        store = PrivateDownloadStore(context)
    }

    @Test
    fun missingRemoteIdIsNotDownloaded() {
        assertFalse(store.hasLocalCopy("never-downloaded-${System.nanoTime()}"))
    }

    @Test
    fun deleteRemovesTargetAndPartial() {
        val id = "del-${System.nanoTime()}"
        val target = store.fileForRemoteId(id)
        val partial = store.partialFileForRemoteId(id)
        target.parentFile?.mkdirs()
        target.writeBytes(ByteArray(128) { 1 })
        partial.writeBytes(ByteArray(32) { 2 })
        assertTrue(target.exists())
        assertTrue(partial.exists())

        store.delete(id)

        assertFalse(target.exists())
        assertFalse(partial.exists())
        assertFalse(store.hasLocalCopy(id))
    }

    @Test
    fun hasLocalCopyRequiresMoreThanTinyFile() {
        val id = "tiny-${System.nanoTime()}"
        val target = store.fileForRemoteId(id)
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(1, 2, 3)) // below MIN_VALID_BYTES (64)
        assertFalse(store.hasLocalCopy(id))
        target.delete()
    }

    @Test
    fun hasLocalCopyTrueForValidSizedFile() {
        val id = "ok-${System.nanoTime()}"
        val target = store.fileForRemoteId(id)
        target.parentFile?.mkdirs()
        target.writeBytes(ByteArray(128) { 7 })
        try {
            assertTrue(store.hasLocalCopy(id))
        } finally {
            store.delete(id)
        }
    }

    @Test
    fun opaqueNamesDifferForDifferentRemoteIds() {
        val a = store.fileForRemoteId("episode-a")
        val b = store.fileForRemoteId("episode-b")
        assertTrue(a.name != b.name)
        assertTrue(a.name.startsWith("a_"))
    }

    @Test
    fun availableBytesIsNonNegative() {
        assertTrue(store.availableBytes() >= 0L)
    }
}
