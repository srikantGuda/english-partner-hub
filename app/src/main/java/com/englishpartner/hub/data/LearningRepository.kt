package com.englishpartner.hub.data

import android.net.Uri
import com.englishpartner.hub.data.db.AppDatabase
import com.englishpartner.hub.data.db.Bookmark
import com.englishpartner.hub.data.db.CourseDay
import com.englishpartner.hub.data.db.DayWithStats
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.Note
import com.englishpartner.hub.data.db.VocabWord
import com.englishpartner.hub.data.storage.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class ImportResult(val imported: Int, val failed: List<String>)

/**
 * Single entry point for lesson data. ViewModels talk to this class, never to DAOs directly.
 */
class LearningRepository(
    private val db: AppDatabase,
    private val storage: MediaStorage
) {
    private val dayDao = db.courseDayDao()
    private val materialDao = db.materialDao()
    private val bookmarkDao = db.bookmarkDao()
    private val noteDao = db.noteDao()
    private val vocabDao = db.vocabDao()

    companion object {
        const val DEFAULT_DAY_COUNT = 60
    }

    /** First launch: create Day 1 to Day 60 so the library is ready to fill. */
    suspend fun ensureSeeded() {
        withContext(Dispatchers.IO) {
            if (dayDao.count() == 0) {
                dayDao.insertAll((1..DEFAULT_DAY_COUNT).map { CourseDay(dayNumber = it) })
            }
        }
    }

    // ---- Days ----
    fun observeDaysWithStats(): Flow<List<DayWithStats>> = dayDao.observeDaysWithStats()
    fun observeDays(): Flow<List<CourseDay>> = dayDao.observeDays()
    fun observeDay(id: Long): Flow<CourseDay?> = dayDao.observeDay(id)
    fun observeCompletedDayCount(): Flow<Int> = dayDao.observeCompletedCount()
    fun observeDayCount(): Flow<Int> = dayDao.observeCount()
    suspend fun getDay(id: Long): CourseDay? = dayDao.getDay(id)

    /** Adds the next day after the highest existing number (Day 61, 62, ...). */
    suspend fun addNextDay(): Long {
        val next = dayDao.maxDayNumber() + 1
        return dayDao.insert(CourseDay(dayNumber = next))
    }

    /** Returns the id of the day with this number, creating it if needed. */
    suspend fun getOrCreateDay(number: Int): Long {
        dayDao.getByNumber(number)?.let { return it.id }
        val id = dayDao.insert(CourseDay(dayNumber = number))
        return if (id > 0) id else dayDao.getByNumber(number)?.id ?: error("Could not create day $number")
    }

    suspend fun renameDay(day: CourseDay, title: String) = dayDao.update(day.copy(title = title.trim()))

    suspend fun setDayCompleted(day: CourseDay, completed: Boolean) = dayDao.update(
        day.copy(completed = completed, completedAt = if (completed) System.currentTimeMillis() else null)
    )

    suspend fun deleteDay(day: CourseDay) {
        withContext(Dispatchers.IO) {
            materialDao.getAll().filter { it.dayId == day.id }.forEach { storage.delete(it.fileName) }
            dayDao.delete(day) // cascades to materials, bookmarks and notes
        }
    }

    // ---- Materials ----
    fun observeMaterials(dayId: Long): Flow<List<Material>> = materialDao.observeForDay(dayId)
    fun observeMaterial(id: Long): Flow<Material?> = materialDao.observe(id)
    fun observeRecentMaterials(limit: Int = 5): Flow<List<Material>> = materialDao.observeRecent(limit)
    fun observeMaterialCount(): Flow<Int> = materialDao.observeCount()
    fun observeCompletedMaterialCount(): Flow<Int> = materialDao.observeCompletedCount()
    suspend fun getMaterial(id: Long): Material? = materialDao.get(id)

    fun fileFor(material: Material): File = storage.fileFor(material.fileName)

    /** Copy picked/shared files into app storage and attach them to a day. */
    suspend fun importUris(dayId: Long, uris: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var ok = 0
        val failed = mutableListOf<String>()
        for (uri in uris) {
            try {
                val stored = storage.copyIn(uri)
                materialDao.insert(
                    Material(
                        dayId = dayId,
                        type = stored.type.name,
                        displayName = stored.displayName,
                        fileName = stored.fileName,
                        mimeType = stored.mimeType,
                        sizeBytes = stored.sizeBytes
                    )
                )
                ok++
            } catch (t: Throwable) {
                failed += (uri.lastPathSegment ?: uri.toString())
            }
        }
        ImportResult(ok, failed)
    }

    /** Save shared plain text as an article for a day. */
    suspend fun importText(dayId: Long, title: String, text: String): Long = withContext(Dispatchers.IO) {
        val stored = storage.saveText(title, text)
        materialDao.insert(
            Material(
                dayId = dayId,
                type = stored.type.name,
                displayName = stored.displayName,
                fileName = stored.fileName,
                mimeType = stored.mimeType,
                sizeBytes = stored.sizeBytes
            )
        )
    }

    suspend fun renameMaterial(material: Material, name: String) =
        materialDao.update(material.copy(displayName = name.trim().ifEmpty { material.displayName }))

    suspend fun moveMaterial(material: Material, toDayId: Long) =
        materialDao.update(material.copy(dayId = toDayId))

    suspend fun deleteMaterial(material: Material) {
        withContext(Dispatchers.IO) {
            materialDao.delete(material)
            storage.delete(material.fileName)
        }
    }

    suspend fun savePlayback(id: Long, positionMs: Long, durationMs: Long) =
        materialDao.savePlayback(id, positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0), System.currentTimeMillis())

    suspend fun saveReadingPosition(id: Long, page: Int) =
        materialDao.saveReadingPosition(id, page.coerceAtLeast(0), System.currentTimeMillis())

    suspend fun markOpened(id: Long) = materialDao.markOpened(id, System.currentTimeMillis())

    suspend fun setMaterialCompleted(id: Long, completed: Boolean) = materialDao.setCompleted(id, completed)

    fun storageBytes(): Long = storage.totalBytes()

    // ---- Bookmarks ----
    fun observeBookmarks(materialId: Long): Flow<List<Bookmark>> = bookmarkDao.observeForMaterial(materialId)
    suspend fun addBookmark(materialId: Long, position: Long, label: String) =
        bookmarkDao.insert(Bookmark(materialId = materialId, position = position, label = label))
    suspend fun deleteBookmark(bookmark: Bookmark) = bookmarkDao.delete(bookmark)

    // ---- Notes ----
    fun observeNotes(dayId: Long): Flow<List<Note>> = noteDao.observeForDay(dayId)
    suspend fun saveNote(dayId: Long, existing: Note?, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (existing == null) noteDao.insert(Note(dayId = dayId, text = trimmed))
        else noteDao.update(existing.copy(text = trimmed, updatedAt = System.currentTimeMillis()))
    }
    suspend fun deleteNote(note: Note) = noteDao.delete(note)

    // ---- Vocabulary ----
    fun searchVocabulary(query: String): Flow<List<VocabWord>> = vocabDao.observeSearch(query.trim())
    fun observeVocabForDay(dayId: Long): Flow<List<VocabWord>> = vocabDao.observeForDay(dayId)
    fun observeVocabCount(): Flow<Int> = vocabDao.observeCount()
    suspend fun saveWord(word: VocabWord) {
        if (word.word.isBlank()) return
        val clean = word.copy(word = word.word.trim(), meaning = word.meaning.trim(), example = word.example.trim())
        if (clean.id == 0L) vocabDao.insert(clean) else vocabDao.update(clean)
    }
    suspend fun deleteWord(word: VocabWord) = vocabDao.delete(word)
}
