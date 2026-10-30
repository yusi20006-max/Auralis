package com.auralis.app.data.download

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Stores audio only inside the app private sandbox ([Context.getFilesDir]).
 * Files are NOT placed in public Downloads/Music and are not indexed by MediaStore,
 * so typical file managers cannot browse or search them.
 *
 * Filenames are opaque hashes (no original title) for extra obscurity.
 */
class PrivateDownloadStore(context: Context) {

    private val root: File = File(context.filesDir, DIR_NAME).also { it.mkdirs() }

    fun fileForRemoteId(remoteId: String): File =
        File(root, opaqueName(remoteId))

    fun hasLocalCopy(remoteId: String): Boolean {
        val f = fileForRemoteId(remoteId)
        return f.isFile && f.length() > 0L
    }

    fun uriForRemoteId(remoteId: String): Uri? {
        val f = fileForRemoteId(remoteId)
        return if (f.isFile && f.length() > 0L) Uri.fromFile(f) else null
    }

    fun delete(remoteId: String) {
        fileForRemoteId(remoteId).delete()
    }

    /**
     * Download [url] into private storage. Returns file:// Uri on success.
     */
    fun download(remoteId: String, url: String): Uri {
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
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            return Uri.fromFile(target)
        } finally {
            connection.disconnect()
            if (partial.exists() && !target.exists()) {
                partial.delete()
            }
        }
    }

    companion object {
        private const val DIR_NAME = "auralis_private"

        private fun opaqueName(remoteId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(remoteId.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { b -> "%02x".format(b) }.take(32)
            // No media extension → less likely to be auto-scanned if path leaked.
            return "a_$hex"
        }
    }
}
