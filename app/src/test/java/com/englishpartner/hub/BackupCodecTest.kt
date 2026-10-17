package com.englishpartner.hub

import com.englishpartner.hub.data.backup.BackupCodec
import com.englishpartner.hub.data.backup.BackupData
import com.englishpartner.hub.data.db.Bookmark
import com.englishpartner.hub.data.db.CourseDay
import com.englishpartner.hub.data.db.Material
import com.englishpartner.hub.data.db.Note
import com.englishpartner.hub.data.db.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {

    private val sample = BackupData(
        days = listOf(
            CourseDay(id = 1, dayNumber = 1, title = "Greetings", completed = true, createdAt = 10, completedAt = 20),
            CourseDay(id = 2, dayNumber = 61, createdAt = 30, completedAt = null)
        ),
        materials = listOf(
            Material(
                id = 5, dayId = 1, type = "AUDIO", displayName = "Day 1 \"audio\".opus", fileName = "abc.opus",
                mimeType = "audio/ogg", sizeBytes = 1234, positionMs = 5000, durationMs = 60000,
                lastPage = 0, completed = false, importedAt = 40, lastOpenedAt = null
            )
        ),
        bookmarks = listOf(Bookmark(id = 9, materialId = 5, position = 3000, label = "Important", createdAt = 50)),
        notes = listOf(Note(id = 3, dayId = 1, text = "Line 1\nLine 2 — ünïcødé", createdAt = 60, updatedAt = 70)),
        vocabulary = listOf(
            VocabWord(id = 4, word = "however", meaning = "lekin", example = "However, I agree.", dayId = 1, mastered = true, createdAt = 80),
            VocabWord(id = 6, word = "although", dayId = null, createdAt = 90)
        )
    )

    @Test
    fun roundTripKeepsEverything() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample, createdAt = 1))
        assertEquals(sample, decoded)
    }

    @Test
    fun rejectsForeignJson() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"format":"something-else","version":1}""")
        }
    }

    @Test
    fun rejectsNewerVersions() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"format":"english-partner-backup","version":999}""")
        }
    }
}
