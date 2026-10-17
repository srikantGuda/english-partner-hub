package com.englishpartner.hub.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.englishpartner.hub.data.db.AppDatabase
import com.englishpartner.hub.data.storage.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(val days: Int, val materials: Int, val files: Int)

/**
 * Local backup to a single .zip that the user saves wherever they like through Android's file picker
 * (phone storage, SD card, USB drive...). Nothing is uploaded anywhere.
 *
 * Zip layout:
 *   backup.json        - all Room data
 *   media/<fileName>   - every imported file
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val storage: MediaStorage
) {
    private companion object {
        const val JSON_ENTRY = "backup.json"
        const val MEDIA_PREFIX = "media/"
    }

    suspend fun export(target: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val data = BackupData(
            days = db.courseDayDao().getAll(),
            materials = db.materialDao().getAll(),
            bookmarks = db.bookmarkDao().getAll(),
            notes = db.noteDao().getAll(),
            vocabulary = db.vocabDao().getAll()
        )
        val json = BackupCodec.encode(data)
        var fileCount = 0
        val out = context.contentResolver.openOutputStream(target, "w")
            ?: throw IllegalStateException("Cannot write to the chosen location.")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.setLevel(Deflater.BEST_SPEED)
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (m in data.materials) {
                val f = storage.fileFor(m.fileName)
                if (!f.isFile) continue
                zip.putNextEntry(ZipEntry(MEDIA_PREFIX + m.fileName))
                f.inputStream().use { it.copyTo(zip, 64 * 1024) }
                zip.closeEntry()
                fileCount++
            }
        }
        BackupSummary(data.days.size, data.materials.size, fileCount)
    }

    /** Replaces all current data with the backup. Files are staged first so a broken zip changes nothing. */
    suspend fun restore(source: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore_staging").apply {
            deleteRecursively(); mkdirs()
        }
        var json: String? = null
        try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IllegalStateException("Cannot open the backup file.")
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        entry.isDirectory -> Unit
                        name == JSON_ENTRY -> json = zip.readBytes().toString(Charsets.UTF_8)
                        name.startsWith(MEDIA_PREFIX) -> {
                            val fileName = name.removePrefix(MEDIA_PREFIX)
                            // Protect against path traversal ("zip slip").
                            if (fileName.isNotBlank() && !fileName.contains('/') &&
                                !fileName.contains('\\') && fileName != "." && fileName != ".."
                            ) {
                                File(staging, fileName).outputStream().use { zip.copyTo(it, 64 * 1024) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val data = BackupCodec.decode(json ?: throw IllegalArgumentException("backup.json is missing."))

            db.withTransaction {
                db.bookmarkDao().deleteAll()
                db.noteDao().deleteAll()
                db.vocabDao().deleteAll()
                db.materialDao().deleteAll()
                db.courseDayDao().deleteAll()
                db.courseDayDao().insertAll(data.days)
                db.materialDao().insertAll(data.materials)
                db.bookmarkDao().insertAll(data.bookmarks)
                db.noteDao().insertAll(data.notes)
                db.vocabDao().insertAll(data.vocabulary)
            }

            // Swap media: remove old files, move staged ones in.
            storage.mediaDir.listFiles()?.forEach { it.delete() }
            var count = 0
            staging.listFiles()?.forEach { f ->
                val dest = File(storage.mediaDir, f.name)
                if (!f.renameTo(dest)) {
                    f.copyTo(dest, overwrite = true)
                    f.delete()
                }
                count++
            }
            BackupSummary(data.days.size, data.materials.size, count)
        } finally {
            staging.deleteRecursively()
        }
    }
}
