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
 * App-private audio storage (Issue #15).
 * - Not in public Downloads / MediaStore
 * - Opaque names
 * - Cancel + .part cleanup
 * - Minimum free-space guard
 * - Final file validation
 */
class PrivateDownloadStore(private val context: Context) {

    private val root: File = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
    private val cancelFlags = ConcurrentHashMap<String, AtomicBoolean>()

    fun fileForRemoteId(remoteId: String): File =
        File(root, opaqueName(remoteId))

    fun partialFileForRemoteId(remoteId: String): File =
        File(root, opaqueName(remoteId) + ".part")

    fun hasLocalCopy(remoteId: String): Boolean {
        val f = fileForRemoteId(remoteId)
        return f.isFile && f.length() > MIN_VALID_BYTES
    }

    fun uriForRemoteId(remoteId: String): Uri? {
        val f = fileForRemoteId(remoteId)
        if (!f.isFile || f.length() <= MIN_VALID_BYTES) return null
        return contentUri(f)
    }

    fun delete(remoteId: String) {
        cancel(remoteId)
        fileForRemoteId(remoteId).delete()
        partialFileForRemoteId(remoteId).delete()
        cancelFlags.remove(remoteId)
    }

    /** Request cancel; in-flight [download] will stop and delete the .part file. */
    fun cancel(remoteId: String) {
        cancelFlags.getOrPut(remoteId) { AtomicBoolean(false) }.set(true)
    }

    fun clearCancel(remoteId: String) {
        cancelFlags[remoteId]?.set(false)
    }

    fun availableBytes(): Long = try {
        StatFs(root.absolutePath).availableBytes
    } catch (_: Exception) {
        Long.MAX_VALUE
    }

    /**
     * @throws DownloadCancelledException
     * @throws InsufficientSpaceException
     * @throws IllegalStateException on HTTP / validation failure
     */
    fun download(
        remoteId: String,
        url: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Uri {
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Only http(s) URLs are supported"
        }

        clearCancel(remoteId)
        val flag = cancelFlags.getOrPut(remoteId) { AtomicBoolean(false) }
        flag.set(false)

        val free = availableBytes()
        if (free < MIN_FREE_BYTES) {
            throw InsufficientSpaceException(free)
        }

        val target = fileForRemoteId(remoteId)
        val partial = partialFileForRemoteId(remoteId)
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
            cancelFlags.remove(remoteId)
        }
    }

    private fun contentUri(file: File): Uri {
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    companion object {
        private const val DIR_NAME = "auralis_private"
        private const val MIN_VALID_BYTES = 64L
        /** Keep a small reserve so the app does not fill storage completely. */
        private const val MIN_FREE_BYTES = 5L * 1024L * 1024L

        private fun opaqueName(remoteId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(remoteId.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { b -> "%02x".format(b) }.take(32)
            return "a_$hex"
        }
    }
}

class DownloadCancelledException(val remoteId: String) :
    Exception("Download cancelled: $remoteId")

class InsufficientSpaceException(val availableBytes: Long) :
    Exception("Insufficient space: $availableBytes")
