package com.auralis.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Imports local audio files (single URI or a document tree) into the Library
 * with best-effort title, duration and artwork extraction.
 *
 * Artwork sources (in order):
 * 1. Embedded picture in the audio container (ID3 APIC, M4A covr, …)
 * 2. Image file next to the audio in the same folder (cover.jpg, folder.png, …)
 *
 * Extracted / copied bytes are stored under app-private filesDir/artwork/ and
 * exposed via FileProvider so Coil can load them reliably.
 */
object LocalAudioImport {

    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "wma", "mp4",
    )

    private val coverNames = setOf(
        "cover.jpg", "cover.jpeg", "cover.png", "cover.webp",
        "folder.jpg", "folder.jpeg", "folder.png",
        "albumart.jpg", "albumart.jpeg", "albumart.png",
        "album.jpg", "album.jpeg", "album.png",
        "front.jpg", "front.jpeg", "front.png",
        "artwork.jpg", "artwork.jpeg", "artwork.png",
        "poster.jpg", "poster.jpeg", "poster.png",
    )

    suspend fun importOne(
        context: Context,
        uri: Uri,
        repository: LibraryRepository,
        parentDir: DocumentFile? = null,
    ): Episode = withContext(Dispatchers.IO) {
        val meta = readMetadata(context, uri, parentDir)
        repository.addLocalEpisode(
            uri = uri,
            title = meta.title,
            durationMs = meta.durationMs,
            artworkUri = meta.artworkUri,
        )
    }

    /**
     * Walks a persisted tree URI and imports each audio file (depth ≤ 6, max 200).
     */
    suspend fun importTree(
        context: Context,
        treeUri: Uri,
        repository: LibraryRepository,
        maxFiles: Int = 200,
    ): List<Episode> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val files = mutableListOf<Pair<DocumentFile, DocumentFile?>>()
        collectAudioFiles(root, files, maxFiles, parent = null)
        files.mapNotNull { (doc, parent) ->
            try {
                importOne(context, doc.uri, repository, parentDir = parent)
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
        out: MutableList<Pair<DocumentFile, DocumentFile?>>,
        maxFiles: Int,
        parent: DocumentFile?,
        depth: Int = 0,
    ) {
        if (out.size >= maxFiles || depth > 6) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (out.size >= maxFiles) return
            when {
                child.isDirectory -> collectAudioFiles(child, out, maxFiles, parent = child, depth = depth + 1)
                child.isFile && isLikelyAudio(child.name, child.type) -> out.add(child to dir)
            }
        }
    }

    private data class LocalMeta(
        val title: String,
        val durationMs: Long,
        val artworkUri: Uri?,
    )

    private fun readMetadata(
        context: Context,
        uri: Uri,
        parentDir: DocumentFile?,
    ): LocalMeta {
        val displayName = queryDisplayName(context, uri)
            ?: uri.lastPathSegment
            ?: "فایل صوتی"
        val titleFromName = displayName.substringBeforeLast('.').ifBlank { displayName }

        var durationMs = 0L
        var embeddedTitle: String? = null
        var artworkUri: Uri? = null

        // 1) Embedded art + duration via MediaMetadataRetriever (prefer FileDescriptor for SAF).
        val retriever = MediaMetadataRetriever()
        try {
            val opened = openRetriever(context, uri, retriever)
            if (opened) {
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.coerceAtLeast(0L)
                    ?: 0L
                embeddedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    artworkUri = persistArtworkBytes(context, uri, picture)
                }
            }
        } catch (_: Exception) {
            // Best-effort: keep filename title / zero duration.
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        // 2) Folder cover fallback (cover.jpg / folder.png / … next to the audio).
        if (artworkUri == null) {
            artworkUri = findFolderCover(context, uri, parentDir)
        }

        return LocalMeta(
            title = embeddedTitle ?: titleFromName,
            durationMs = durationMs,
            artworkUri = artworkUri,
        )
    }

    /**
     * Prefer openFileDescriptor — more reliable for SAF/tree URIs than setDataSource(context, uri).
     */
    private fun openRetriever(context: Context, uri: Uri, retriever: MediaMetadataRetriever): Boolean {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                return true
            }
        } catch (_: Exception) {
        }
        return try {
            retriever.setDataSource(context, uri)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findFolderCover(
        context: Context,
        audioUri: Uri,
        parentDir: DocumentFile?,
    ): Uri? {
        val dir = parentDir
            ?: DocumentFile.fromSingleUri(context, audioUri)?.parentFile
            ?: return null
        val children = dir.listFiles() ?: return null
        // Exact well-known names first (case-insensitive).
        val byName = children.firstOrNull { child ->
            child.isFile && child.name?.lowercase(Locale.US) in coverNames
        }
        if (byName != null) {
            return copyCoverDocument(context, audioUri, byName)
        }
        // Any image file in the same folder as a last resort.
        val anyImage = children.firstOrNull { child ->
            if (!child.isFile) return@firstOrNull false
            val n = child.name?.lowercase(Locale.US).orEmpty()
            val mime = child.type?.lowercase(Locale.US).orEmpty()
            mime.startsWith("image/") ||
                n.endsWith(".jpg") || n.endsWith(".jpeg") ||
                n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".bmp")
        }
        return anyImage?.let { copyCoverDocument(context, audioUri, it) }
    }

    private fun copyCoverDocument(context: Context, sourceUri: Uri, cover: DocumentFile): Uri? {
        return try {
            context.contentResolver.openInputStream(cover.uri)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.isEmpty()) null else persistArtworkBytes(context, sourceUri, bytes)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun persistArtworkBytes(context: Context, sourceUri: Uri, bytes: ByteArray): Uri? {
        return try {
            val dir = File(context.filesDir, "artwork").apply { mkdirs() }
            val safe = Integer.toHexString(sourceUri.toString().hashCode())
            val ext = sniffImageExtension(bytes)
            val file = File(dir, "$safe.$ext")
            file.outputStream().use { it.write(bytes) }
            // Prefer FileProvider so Coil / system components never need raw file:// access.
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sniffImageExtension(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return "jpg"
        }
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return "png"
        }
        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte()
        ) {
            return "webp"
        }
        return "jpg"
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
