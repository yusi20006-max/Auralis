package com.auralis.app.data.download

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    fun sourceSpecificDeleteDoesNotRemoveAmbiguousLegacyFile() {
        val remoteId = "ambiguous-legacy-${System.nanoTime()}"
        val sourceFile = store.fileForIdentity("catalog:auralis", remoteId)
        val legacyFile = store.fileForRemoteId(remoteId)
        sourceFile.parentFile?.mkdirs()
        sourceFile.writeBytes(ByteArray(128) { 3 })
        legacyFile.writeBytes(ByteArray(128) { 4 })

        store.delete("catalog:auralis", remoteId)

        assertFalse("the exact source-owned file should be deleted", sourceFile.exists())
        assertTrue("an ambiguous legacy file must be preserved", legacyFile.exists())
        legacyFile.delete()
    }

    @Test
    fun hasLocalCopyRequiresMoreThanTinyFile() {
        val id = "tiny-${System.nanoTime()}"
        val target = store.fileForRemoteId(id)
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(1, 2, 3))
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

    @Test
    fun sameRemoteIdDifferentSourcesUseDistinctFiles() {
        val remoteId = "shared-remote-${System.nanoTime()}"
        val catalogFile = store.fileForIdentity("catalog:auralis", remoteId)
        val rssFile = store.fileForIdentity("rss:feed-1", remoteId)
        assertNotEquals(catalogFile.absolutePath, rssFile.absolutePath)

        catalogFile.parentFile?.mkdirs()
        catalogFile.writeBytes(ByteArray(128) { 1 })
        rssFile.writeBytes(ByteArray(128) { 2 })
        try {
            assertTrue(store.hasLocalCopy("catalog:auralis", remoteId))
            assertTrue(store.hasLocalCopy("rss:feed-1", remoteId))
            val catalogUri = store.uriForIdentity("catalog:auralis", remoteId)
            val rssUri = store.uriForIdentity("rss:feed-1", remoteId)
            assertNotNull(catalogUri)
            assertNotNull(rssUri)
            assertNotEquals(catalogUri, rssUri)
        } finally {
            store.delete("catalog:auralis", remoteId)
            store.delete("rss:feed-1", remoteId)
        }
    }

    @Test
    fun identityLookupFallsBackToLegacyRemoteIdOnlyFile() {
        val remoteId = "legacy-file-${System.nanoTime()}"
        val legacy = store.fileForRemoteId(remoteId)
        val primary = store.fileForIdentity("catalog:auralis", remoteId)
        legacy.parentFile?.mkdirs()
        legacy.writeBytes(ByteArray(128) { 9 })
        try {
            assertFalse("primary composite path must not exist yet", primary.exists())
            assertTrue(legacy.isFile && legacy.length() > 64)
            assertTrue(store.hasLocalCopy("catalog:auralis", remoteId))
            val uri = store.uriForIdentity("catalog:auralis", remoteId)
            assertNotNull("legacy file must resolve to a playable URI", uri)
            assertTrue(
                uri!!.scheme == "content" || uri.scheme == "file",
            )
        } finally {
            store.delete("catalog:auralis", remoteId)
        }
    }

    @Test
    fun storageKeyIncludesSourceWhenPresent() {
        assertEquals("remote-only", store.storageKey(null, "remote-only"))
        assertEquals("remote-only", store.storageKey("  ", "remote-only"))
        assertEquals("catalog:auralis::ep-1", store.storageKey("catalog:auralis", "ep-1"))
    }
}
