package com.englishpartner.hub.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDayDao {
    @Query(
        """
        SELECT d.*,
               COUNT(m.id) AS materialCount,
               COALESCE(SUM(CASE WHEN m.completed = 1 THEN 1 ELSE 0 END), 0) AS completedCount
        FROM course_days d
        LEFT JOIN materials m ON m.dayId = d.id
        GROUP BY d.id
        ORDER BY d.dayNumber ASC
        """
    )
    fun observeDaysWithStats(): Flow<List<DayWithStats>>

    @Query("SELECT * FROM course_days ORDER BY dayNumber ASC")
    fun observeDays(): Flow<List<CourseDay>>

    @Query("SELECT * FROM course_days ORDER BY dayNumber ASC")
    suspend fun getAll(): List<CourseDay>

    @Query("SELECT * FROM course_days WHERE id = :id")
    fun observeDay(id: Long): Flow<CourseDay?>

    @Query("SELECT * FROM course_days WHERE id = :id")
    suspend fun getDay(id: Long): CourseDay?

    @Query("SELECT * FROM course_days WHERE dayNumber = :number LIMIT 1")
    suspend fun getByNumber(number: Int): CourseDay?

    @Query("SELECT COUNT(*) FROM course_days")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(dayNumber), 0) FROM course_days")
    suspend fun maxDayNumber(): Int

    @Query("SELECT COUNT(*) FROM course_days WHERE completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM course_days")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: CourseDay): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(days: List<CourseDay>)

    @Update
    suspend fun update(day: CourseDay)

    @Delete
    suspend fun delete(day: CourseDay)

    @Query("DELETE FROM course_days")
    suspend fun deleteAll()
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE dayId = :dayId ORDER BY importedAt ASC")
    fun observeForDay(dayId: Long): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE id = :id")
    fun observe(id: Long): Flow<Material?>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun get(id: Long): Material?

    @Query("SELECT * FROM materials")
    suspend fun getAll(): List<Material>

    @Query("SELECT * FROM materials WHERE lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Material>>

    @Query("SELECT COUNT(*) FROM materials")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM materials WHERE completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Insert
    suspend fun insert(material: Material): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(materials: List<Material>)

    @Update
    suspend fun update(material: Material)

    @Query("UPDATE materials SET positionMs = :positionMs, durationMs = :durationMs, lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun savePlayback(id: Long, positionMs: Long, durationMs: Long, openedAt: Long)

    @Query("UPDATE materials SET lastPage = :page, lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun saveReadingPosition(id: Long, page: Int, openedAt: Long)

    @Query("UPDATE materials SET lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun markOpened(id: Long, openedAt: Long)

    @Query("UPDATE materials SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Delete
    suspend fun delete(material: Material)

    @Query("DELETE FROM materials")
    suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE materialId = :materialId ORDER BY position ASC")
    fun observeForMaterial(materialId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<Bookmark>

    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<Bookmark>)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE dayId = :dayId ORDER BY updatedAt DESC")
    fun observeForDay(dayId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<Note>

    @Insert
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}

@Dao
interface VocabDao {
    @Query(
        """
        SELECT * FROM vocabulary
        WHERE (:query = '' OR word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%')
        ORDER BY word COLLATE NOCASE ASC
        """
    )
    fun observeSearch(query: String): Flow<List<VocabWord>>

    @Query("SELECT * FROM vocabulary WHERE dayId = :dayId ORDER BY word COLLATE NOCASE ASC")
    fun observeForDay(dayId: Long): Flow<List<VocabWord>>

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM vocabulary")
    suspend fun getAll(): List<VocabWord>

    @Insert
    suspend fun insert(word: VocabWord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<VocabWord>)

    @Update
    suspend fun update(word: VocabWord)

    @Delete
    suspend fun delete(word: VocabWord)

    @Query("DELETE FROM vocabulary")
    suspend fun deleteAll()
}
