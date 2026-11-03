package com.auralis.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Imports local audio files (single URI or a document tree) into the Library
 * with best-effort title, duration and embedded artwork extraction.
 */
object LocalAudioImport {

    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "wma", "mp4",
    )

    suspend fun importOne(
        context: Context,
        uri: Uri,
        repository: LibraryRepository,
    ): Episode = withContext(Dispatchers.IO) {
        val meta = readMetadata(context, uri)
        repository.addLocalEpisode(
            uri = uri,
            title = meta.title,
            durationMs = meta.durationMs,
            artworkUri = meta.artworkUri,
        )
    }

    /**
     * Walks a persisted tree URI (one level of folders deep enough to catch
     * nested album folders up to a safe limit) and imports each audio file.
     */
    suspend fun importTree(
        context: Context,
        treeUri: Uri,
        repository: LibraryRepository,
        maxFiles: Int = 200,
    ): List<Episode> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val files = mutableListOf<DocumentFile>()
        collectAudioFiles(root, files, maxFiles)
        files.mapNotNull { doc ->
            val uri = doc.uri
            try {
                importOne(context, uri, repository)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun isLikelyAudio(name: String?, mimeType: String?): Boolean {
        val mime = mimeType?.lowercase(Locale.US).orEmpty()
        if (mime.startsWith("audio/")) return true
        if (mime == "application/ogg") return true
        val ext = name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.US)
            .orEmpty()
        return ext in audioExtensions
    }

    private fun collectAudioFiles(
        dir: DocumentFile,
        out: MutableList<DocumentFile>,
        maxFiles: Int,
        depth: Int = 0,
    ) {
        if (out.size >= maxFiles || depth > 6) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (out.size >= maxFiles) return
            when {
                child.isDirectory -> collectAudioFiles(child, out, maxFiles, depth + 1)
                child.isFile && isLikelyAudio(child.name, child.type) -> out.add(child)
            }
        }
    }

    private data class LocalMeta(
        val title: String,
        val durationMs: Long,
        val artworkUri: Uri?,
    )

    private fun readMetadata(context: Context, uri: Uri): LocalMeta {
        val displayName = queryDisplayName(context, uri)
            ?: uri.lastPathSegment
            ?: "فایل صوتی"
        val titleFromName = displayName.substringBeforeLast('.').ifBlank { displayName }

        var durationMs = 0L
        var embeddedTitle: String? = null
        var artworkUri: Uri? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            embeddedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                artworkUri = persistArtwork(context, uri, picture)
            }
        } catch (_: Exception) {
            // Keep filename-based title and zero duration.
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        return LocalMeta(
            title = embeddedTitle ?: titleFromName,
            durationMs = durationMs,
            artworkUri = artworkUri,
        )
    }

    private fun persistArtwork(context: Context, sourceUri: Uri, bytes: ByteArray): Uri? {
        return try {
            val dir = File(context.filesDir, "artwork").apply { mkdirs() }
            val safe = Integer.toHexString(sourceUri.toString().hashCode())
            val file = File(dir, "$safe.jpg")
            file.outputStream().use { it.write(bytes) }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") {
            return uri.lastPathSegment
        }
        return try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column != -1 && cursor.moveToFirst()) {
                        cursor.getString(column)?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                }
        } catch (_: Exception) {
            null
        }
    }
}
