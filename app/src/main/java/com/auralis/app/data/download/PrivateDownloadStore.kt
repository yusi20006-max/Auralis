package com.auralis.app.data.download

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * App-private audio storage (Issue #15 / Stage 4).
 * - Opaque names derived from stable identity
 * - Primary key: (sourceId, remoteId); legacy remoteId-only files still resolved
 * - Cancel + .part cleanup, free-space guard, final validation
 */
class PrivateDownloadStore(private val context: Context) {

    private val root: File = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
    private val cancelFlags = ConcurrentHashMap<String, AtomicBoolean>()

    fun storageKey(sourceId: String?, remoteId: String): String {
        val sid = sourceId?.trim().orEmpty()
        return if (sid.isEmpty()) remoteId else "$sid::$remoteId"
    }

    fun fileForRemoteId(remoteId: String): File =
        fileForKey(storageKey(null, remoteId))

    fun fileForIdentity(sourceId: String?, remoteId: String): File =
        fileForKey(storageKey(sourceId, remoteId))

    fun partialFileForRemoteId(remoteId: String): File =
        File(root, opaqueName(storageKey(null, remoteId)) + ".part")

    fun partialFileForIdentity(sourceId: String?, remoteId: String): File =
        File(root, opaqueName(storageKey(sourceId, remoteId)) + ".part")

    fun hasLocalCopy(remoteId: String): Boolean =
        hasLocalCopy(sourceId = null, remoteId = remoteId)

    fun hasLocalCopy(sourceId: String?, remoteId: String): Boolean {
        val primary = fileForIdentity(sourceId, remoteId)
        if (primary.isFile && primary.length() > MIN_VALID_BYTES) return true
        if (!sourceId.isNullOrBlank()) {
            val legacy = fileForRemoteId(remoteId)
            if (legacy.isFile && legacy.length() > MIN_VALID_BYTES) return true
        }
        return false
    }

    fun uriForRemoteId(remoteId: String): Uri? =
        uriForIdentity(sourceId = null, remoteId = remoteId)

    fun uriForIdentity(sourceId: String?, remoteId: String): Uri? {
        val primary = fileForIdentity(sourceId, remoteId)
        if (primary.isFile && primary.length() > MIN_VALID_BYTES) return contentUri(primary)
        if (!sourceId.isNullOrBlank()) {
            val legacy = fileForRemoteId(remoteId)
            if (legacy.isFile && legacy.length() > MIN_VALID_BYTES) return contentUri(legacy)
        }
        return null
    }

    fun delete(remoteId: String) =
        delete(sourceId = null, remoteId = remoteId)

    fun delete(sourceId: String?, remoteId: String) {
        val key = storageKey(sourceId, remoteId)
        cancel(sourceId, remoteId)
        fileForKey(key).delete()
        File(root, opaqueName(key) + ".part").delete()
        cancelFlags.remove(key)
        if (!sourceId.isNullOrBlank()) {
            val legacyKey = storageKey(null, remoteId)
            fileForKey(legacyKey).delete()
            File(root, opaqueName(legacyKey) + ".part").delete()
            cancelFlags.remove(legacyKey)
        }
    }

    fun cancel(remoteId: String) =
        cancel(sourceId = null, remoteId = remoteId)

    fun cancel(sourceId: String?, remoteId: String) {
        val key = storageKey(sourceId, remoteId)
        cancelFlags.getOrPut(key) { AtomicBoolean(false) }.set(true)
        if (!sourceId.isNullOrBlank()) {
            val legacyKey = storageKey(null, remoteId)
            cancelFlags.getOrPut(legacyKey) { AtomicBoolean(false) }.set(true)
        }
    }

    fun clearCancel(remoteId: String) =
        clearCancel(sourceId = null, remoteId = remoteId)

    fun clearCancel(sourceId: String?, remoteId: String) {
        cancelFlags[storageKey(sourceId, remoteId)]?.set(false)
        if (!sourceId.isNullOrBlank()) {
            cancelFlags[storageKey(null, remoteId)]?.set(false)
        }
    }

    fun availableBytes(): Long = try {
        StatFs(root.absolutePath).availableBytes
    } catch (_: Exception) {
        Long.MAX_VALUE
    }

    fun download(
        remoteId: String,
        url: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Uri = download(sourceId = null, remoteId = remoteId, url = url, onProgress = onProgress)

    fun download(
        sourceId: String?,
        remoteId: String,
        url: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Uri {
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Only http(s) URLs are supported"
        }

        val key = storageKey(sourceId, remoteId)
        clearCancel(sourceId, remoteId)
        val flag = cancelFlags.getOrPut(key) { AtomicBoolean(false) }
        flag.set(false)

        val free = availableBytes()
        if (free < MIN_FREE_BYTES) {
            throw InsufficientSpaceException(free)
        }

        val target = fileForKey(key)
        val partial = File(root, opaqueName(key) + ".part")
        if (partial.exists()) partial.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            if (flag.get()) throw DownloadCancelledException(remoteId)
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val total = connection.contentLengthLong.coerceAtLeast(-1L)
            if (total > 0 && free < total + MIN_FREE_BYTES) {
                throw InsufficientSpaceException(free)
            }

            var written = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        if (flag.get()) {
                            throw DownloadCancelledException(remoteId)
                        }
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0 && onProgress != null) {
                            onProgress((written.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }

            if (flag.get()) throw DownloadCancelledException(remoteId)

            if (written <= MIN_VALID_BYTES) {
                partial.delete()
                throw IllegalStateException("Downloaded file too small")
            }
            if (total > 0 && written < total) {
                partial.delete()
                throw IllegalStateException("Incomplete download: $written / $total")
            }

            onProgress?.invoke(1f)
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            if (!target.isFile || target.length() <= MIN_VALID_BYTES) {
                target.delete()
                throw IllegalStateException("Final file validation failed")
            }
            return contentUri(target)
        } catch (e: DownloadCancelledException) {
            partial.delete()
            throw e
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            connection.disconnect()
            cancelFlags.remove(key)
        }
    }

    private fun fileForKey(key: String): File =
        File(root, opaqueName(key))

    /**
     * Prefer FileProvider content URIs for cross-component safety.
     * Fall back to file:// when the provider is unavailable (misconfigured paths
     * or limited unit-test environments) so local playback still works in-process.
     */
    private fun contentUri(file: File): Uri {
        val canonical = try {
            file.canonicalFile
        } catch (_: Exception) {
            file.absoluteFile
        }
        return try {
            val authority = context.packageName + ".fileprovider"
            FileProvider.getUriForFile(context, authority, canonical)
        } catch (_: IllegalArgumentException) {
            Uri.fromFile(canonical)
        }
    }

    companion object {
        private const val DIR_NAME = "auralis_private"
        private const val MIN_VALID_BYTES = 64L
        private const val MIN_FREE_BYTES = 5L * 1024L * 1024L

        private fun opaqueName(key: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(key.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { b -> "%02x".format(b) }.take(32)
            return "a_$hex"
        }
    }
}

class DownloadCancelledException(val remoteId: String) :
    Exception("Download cancelled: $remoteId")

class InsufficientSpaceException(val availableBytes: Long) :
    Exception("Insufficient space: $availableBytes")
