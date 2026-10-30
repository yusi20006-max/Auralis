package com.auralis.app.data.download

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * App-private audio storage. Not in public Downloads; not MediaStore-indexed.
 * Opaque hashed filenames. Playback URIs go through FileProvider (content://).
 */
class PrivateDownloadStore(private val context: Context) {

    private val root: File = File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    fun fileForRemoteId(remoteId: String): File =
        File(root, opaqueName(remoteId))

    fun hasLocalCopy(remoteId: String): Boolean {
        val f = fileForRemoteId(remoteId)
        return f.isFile && f.length() > 0L
    }

    fun uriForRemoteId(remoteId: String): Uri? {
        val f = fileForRemoteId(remoteId)
        if (!f.isFile || f.length() <= 0L) return null
        return contentUri(f)
    }

    fun delete(remoteId: String) {
        fileForRemoteId(remoteId).delete()
    }

    /**
     * @param onProgress fraction 0f..1f (best-effort; unknown length → indeterminate jumps)
     */
    fun download(
        remoteId: String,
        url: String,
        onProgress: ((Float) -> Unit)? = null,
    ): Uri {
        require(url.startsWith("https://") || url.startsWith("http://")) {
            "Only http(s) URLs are supported"
        }
        val target = fileForRemoteId(remoteId)
        val partial = File(root, target.name + ".part")

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val total = connection.contentLengthLong.coerceAtLeast(-1L)
            var written = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
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
            onProgress?.invoke(1f)
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            return contentUri(target)
        } finally {
            connection.disconnect()
            if (partial.exists() && !target.exists()) {
                partial.delete()
            }
        }
    }

    private fun contentUri(file: File): Uri {
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    companion object {
        private const val DIR_NAME = "auralis_private"

        private fun opaqueName(remoteId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(remoteId.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { b -> "%02x".format(b) }.take(32)
            return "a_$hex"
        }
    }
}
