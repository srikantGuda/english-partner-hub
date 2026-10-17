package com.englishpartner.hub.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.englishpartner.hub.data.model.MaterialType
import com.englishpartner.hub.util.Formatters
import java.io.File
import java.util.UUID

/** Information about an incoming file before it is copied. */
data class IncomingFile(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)

/** Result of copying a file into app storage. */
data class StoredFile(
    val fileName: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val type: MaterialType
)

/**
 * Keeps imported materials inside the app's private storage (filesDir/media).
 * Files here are not visible to other apps, never leave the phone, and are removed on uninstall.
 */
class MediaStorage(private val context: Context) {

    val mediaDir: File = File(context.filesDir, "media").apply { mkdirs() }

    fun fileFor(fileName: String): File = File(mediaDir, fileName)

    fun describe(uri: Uri): IncomingFile {
        val resolver = context.contentResolver
        var name: String? = null
        var size = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0 && !cursor.isNull(nameIdx)) name = cursor.getString(nameIdx)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                    }
                }
        }
        val fallbackName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        val finalName = name ?: fallbackName
        var mime = runCatching { resolver.getType(uri) }.getOrNull()
        if (mime.isNullOrBlank() || mime == "application/octet-stream") {
            val ext = MaterialType.extensionOf(finalName)
            val guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            if (!guessed.isNullOrBlank()) mime = guessed
        }
        return IncomingFile(finalName, mime ?: "application/octet-stream", size)
    }

    /** Copy a content:// or file:// Uri into private storage. Must run off the main thread. */
    fun copyIn(uri: Uri): StoredFile {
        val info = describe(uri)
        val type = MaterialType.detect(info.mimeType, info.displayName)
        val ext = MaterialType.extensionOf(info.displayName).ifEmpty {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(info.mimeType) ?: "bin"
        }
        val target = File(mediaDir, "${UUID.randomUUID()}.$ext")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open ${info.displayName}")
        try {
            input.use { inp -> target.outputStream().use { out -> inp.copyTo(out, 64 * 1024) } }
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
        return StoredFile(
            fileName = target.name,
            displayName = info.displayName,
            mimeType = info.mimeType,
            sizeBytes = target.length(),
            type = type
        )
    }

    /** Save shared plain text (e.g. a WhatsApp message) as an article file. */
    fun saveText(title: String, text: String): StoredFile {
        val target = File(mediaDir, "${UUID.randomUUID()}.txt")
        target.writeText(text)
        val name = Formatters.safeFileName(title).let { if (it.endsWith(".txt")) it else "$it.txt" }
        return StoredFile(target.name, name, "text/plain", target.length(), MaterialType.TEXT)
    }

    fun delete(fileName: String) {
        if (fileName.isBlank() || fileName.contains('/')) return
        File(mediaDir, fileName).delete()
    }

    fun totalBytes(): Long = mediaDir.listFiles()?.sumOf { it.length() } ?: 0L
}
