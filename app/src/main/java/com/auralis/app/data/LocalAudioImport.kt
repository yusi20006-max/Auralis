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
import java.io.RandomAccessFile
import java.util.Locale

/**
 * Imports local audio with title, duration and artwork extraction.
 *
 * Artwork order:
 * 1. MediaMetadataRetriever.embeddedPicture
 * 2. Manual MP4/M4A `covr` atom parse (many podcast m4a files)
 * 3. Image next to the audio (cover.jpg, folder.png, …)
 *
 * Cached under filesDir/artwork/ and exposed via FileProvider for Coil.
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

    private val m4aExtensions = setOf("m4a", "mp4", "aac", "m4b")

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
        val ext = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.US)

        var durationMs = 0L
        var embeddedTitle: String? = null
        var artworkBytes: ByteArray? = null

        val retriever = MediaMetadataRetriever()
        try {
            if (openRetriever(context, uri, retriever)) {
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.coerceAtLeast(0L)
                    ?: 0L
                embeddedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    artworkBytes = picture
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        if (artworkBytes == null && (ext in m4aExtensions || looksLikeMpeg4(context, uri))) {
            artworkBytes = extractMp4Cover(context, uri)
        }

        var artworkUri: Uri? = artworkBytes?.let { persistArtworkBytes(context, uri, it) }

        if (artworkUri == null) {
            artworkUri = findFolderCover(context, uri, parentDir)
        }

        return LocalMeta(
            title = embeddedTitle ?: titleFromName,
            durationMs = durationMs,
            artworkUri = artworkUri,
        )
    }

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

    private fun looksLikeMpeg4(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(12)
                val n = input.read(header)
                if (n < 8) return false
                header[4] == 'f'.code.toByte() &&
                    header[5] == 't'.code.toByte() &&
                    header[6] == 'y'.code.toByte() &&
                    header[7] == 'p'.code.toByte()
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun extractMp4Cover(context: Context, uri: Uri): ByteArray? {
        val tmp = File(context.cacheDir, "covr_scan_${uri.toString().hashCode()}.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (!tmp.exists() || tmp.length() < 16L) return null
            return RandomAccessFile(tmp, "r").use { raf ->
                scanCovrAtom(raf) ?: scanCovrLinear(raf)
            }
        } catch (_: Exception) {
            return null
        } finally {
            try {
                tmp.delete()
            } catch (_: Exception) {
            }
        }
    }

    private fun scanCovrAtom(raf: RandomAccessFile): ByteArray? {
        val fileLen = raf.length()
        return walkAtoms(raf, 0L, fileLen, depth = 0)
    }

    private fun walkAtoms(
        raf: RandomAccessFile,
        start: Long,
        end: Long,
        depth: Int,
    ): ByteArray? {
        if (depth > 12) return null
        var offset = start
        while (offset + 8 <= end) {
            raf.seek(offset)
            val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
            val type = readFourCc(raf)
            var header = 8L
            var atomSize = size32
            if (size32 == 1L) {
                if (offset + 16 > end) break
                atomSize = raf.readLong()
                header = 16L
            } else if (size32 == 0L) {
                atomSize = end - offset
            }
            if (atomSize < header || offset + atomSize > end + 1) break

            val dataStart = offset + header
            val dataEnd = offset + atomSize

            when (type) {
                "covr" -> {
                    val fromChild = walkAtoms(raf, dataStart, dataEnd, depth + 1)
                    if (fromChild != null) return fromChild
                    val raw = readAtomPayload(raf, dataStart, dataEnd)
                    if (raw != null && looksLikeImage(raw)) return raw
                }
                "data" -> {
                    if (dataEnd - dataStart >= 8) {
                        raf.seek(dataStart)
                        raf.skipBytes(8)
                        val payloadLen = (dataEnd - dataStart - 8).toInt().coerceAtLeast(0)
                        if (payloadLen in 32..8_000_000) {
                            val payload = ByteArray(payloadLen)
                            raf.readFully(payload)
                            if (looksLikeImage(payload)) return payload
                        }
                    }
                }
                "moov", "udta", "meta", "ilst", "trak", "mdia", "minf", "stbl" -> {
                    val childStart = if (type == "meta") dataStart + 4 else dataStart
                    val found = walkAtoms(raf, childStart.coerceAtMost(dataEnd), dataEnd, depth + 1)
                    if (found != null) return found
                }
            }
            offset += atomSize
        }
        return null
    }

    private fun scanCovrLinear(raf: RandomAccessFile): ByteArray? {
        val len = raf.length()
        if (len > 40_000_000L) return null
        raf.seek(0)
        val reads = mutableListOf(0L to minOf(len, 2_000_000L))
        if (len > 2_000_000L) {
            val start = (len - 2_000_000L).coerceAtLeast(0L)
            reads.add(start to (len - start))
        }
        for ((start, size) in reads) {
            raf.seek(start)
            val buf = ByteArray(size.toInt())
            raf.readFully(buf)
            var i = 0
            while (i + 12 < buf.size) {
                if (buf[i] == 'c'.code.toByte() && buf[i + 1] == 'o'.code.toByte() &&
                    buf[i + 2] == 'v'.code.toByte() && buf[i + 3] == 'r'.code.toByte()
                ) {
                    val abs = start + i + 4
                    val fromData = tryParseDataAtomAt(raf, abs, len)
                    if (fromData != null) return fromData
                }
                i++
            }
        }
        return null
    }

    private fun tryParseDataAtomAt(raf: RandomAccessFile, pos: Long, fileLen: Long): ByteArray? {
        if (pos + 16 > fileLen) return null
        return try {
            raf.seek(pos)
            val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
            val type = readFourCc(raf)
            if (type != "data") return null
            var header = 8L
            var atomSize = size32
            if (size32 == 1L) {
                atomSize = raf.readLong()
                header = 16L
            }
            if (atomSize < header + 8) return null
            val payloadPos = pos + header + 8
            val payloadLen = (atomSize - header - 8).toInt()
            if (payloadLen !in 32..8_000_000) return null
            if (payloadPos + payloadLen > fileLen) return null
            raf.seek(payloadPos)
            val payload = ByteArray(payloadLen)
            raf.readFully(payload)
            if (looksLikeImage(payload)) payload else null
        } catch (_: Exception) {
            null
        }
    }

    private fun readAtomPayload(raf: RandomAccessFile, start: Long, end: Long): ByteArray? {
        val len = (end - start).toInt()
        if (len !in 32..8_000_000) return null
        return try {
            raf.seek(start)
            val bytes = ByteArray(len)
            raf.readFully(bytes)
            if (len > 16 && looksLikeImage(bytes.copyOfRange(8, len))) {
                bytes.copyOfRange(8, len)
            } else if (looksLikeImage(bytes)) {
                bytes
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readFourCc(raf: RandomAccessFile): String {
        val b = ByteArray(4)
        raf.readFully(b)
        return String(b, Charsets.US_ASCII)
    }

    private fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return true
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return true
        }
        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte()
        ) {
            return true
        }
        if (bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) return true
        return false
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
        val byName = children.firstOrNull { child ->
            child.isFile && child.name?.lowercase(Locale.US) in coverNames
        }
        if (byName != null) return copyCoverDocument(context, audioUri, byName)
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
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte()
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
