package com.auralis.app.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Issue #11 — probe whether a local media URI is still readable.
 * Network (http/https) URIs are not probed here; the player handles those.
 */
object LocalUriAccess {

    fun isRemote(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }

    /**
     * Returns true when the app can open [uri] for reading right now.
     * Catches revoked SAF permissions, missing files, and invalid paths without throwing.
     */
    fun canRead(context: Context, uri: Uri): Boolean {
        if (isRemote(uri)) return true
        return try {
            when (uri.scheme?.lowercase()) {
                "file" -> {
                    val path = uri.path ?: return false
                    val file = File(path)
                    file.isFile && file.canRead() && file.length() >= 0L
                }
                "content" -> {
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                }
                else -> {
                    // Unknown scheme: attempt open once.
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                }
            }
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Prefer a readable local URI; otherwise fall back to remote [Episode.audioUrl].
     */
    fun resolvePlayableUri(context: Context, episode: Episode): Uri? {
        val local = episode.localUri
        if (local != null) {
            if (isRemote(local) || canRead(context, local)) return local
        }
        val remote = episode.audioUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        if (remote != null) return remote
        return null
    }

    fun unreadableLocalMessage(): String =
        "فایل در دسترس نیست. ممکن است حذف شده یا دسترسی آن لغو شده باشد. دوباره اضافه کنید."

    fun noSourceMessage(): String =
        "منبع پخش برای این اپیزود وجود ندارد."
}
