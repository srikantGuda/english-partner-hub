package com.englishpartner.hub.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.englishpartner.hub.data.model.MaterialType

/** One class day received on WhatsApp: Day 1, Day 2 ... and any extra days. */
@Entity(
    tableName = "course_days",
    indices = [Index(value = ["dayNumber"], unique = true)]
)
data class CourseDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayNumber: Int,
    val title: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/** A file (audio, video, PDF, image, text) attached to a day. The file lives in app storage. */
@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = CourseDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId")]
)
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val type: String,
    val displayName: String,
    /** File name inside the app's private media folder. Never an absolute path. */
    val fileName: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val lastPage: Int = 0,
    val completed: Boolean = false,
    val importedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
)

/** A saved spot inside a material: page for PDFs, paragraph for articles, milliseconds for media. */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Material::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materialId")]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val position: Long,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Personal notes, attached to a day. */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = CourseDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** A new word or phrase learned. Optionally linked to the day it came from. */
@Entity(
    tableName = "vocabulary",
    foreignKeys = [
        ForeignKey(
            entity = CourseDay::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dayId")]
)
data class VocabWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val meaning: String = "",
    val example: String = "",
    val dayId: Long? = null,
    val mastered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Query result: a day plus counts of its materials. */
data class DayWithStats(
    @Embedded val day: CourseDay,
    @ColumnInfo(name = "materialCount") val materialCount: Int,
    @ColumnInfo(name = "completedCount") val completedCount: Int
)

// Computed helpers live outside the entities so Room never tries to map them to columns.

val CourseDay.displayTitle: String
    get() = if (title.isBlank()) "Day $dayNumber" else "Day $dayNumber · $title"

val Material.materialType: MaterialType
    get() = MaterialType.fromName(type)

/** 0f..1f, used for progress bars on audio/video. */
val Material.playbackFraction: Float
    get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
