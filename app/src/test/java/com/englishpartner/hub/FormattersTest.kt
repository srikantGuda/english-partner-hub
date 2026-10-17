package com.englishpartner.hub

import com.englishpartner.hub.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun formatsDurations() {
        assertEquals("0:00", Formatters.duration(0))
        assertEquals("0:00", Formatters.duration(-5))
        assertEquals("1:05", Formatters.duration(65_000))
        assertEquals("1:02:05", Formatters.duration(3_725_000))
    }

    @Test
    fun formatsFileSizes() {
        assertEquals("512 B", Formatters.fileSize(512))
        assertEquals("2 KB", Formatters.fileSize(2048))
        assertEquals("1.5 MB", Formatters.fileSize(1_572_864))
    }

    @Test
    fun percentIsSafe() {
        assertEquals(0, Formatters.percent(3, 0))
        assertEquals(50, Formatters.percent(30, 60))
        assertEquals(100, Formatters.percent(70, 60))
    }

    @Test
    fun guessesDayNumbers() {
        assertEquals(12, Formatters.guessDayNumber("Day 12 audio.mp3"))
        assertEquals(7, Formatters.guessDayNumber("day_07.pdf"))
        assertEquals(3, Formatters.guessDayNumber("lesson_day-3 notes"))
        assertEquals(61, Formatters.guessDayNumber("DAY61"))
        assertNull(Formatters.guessDayNumber("Birthday party.jpg"))
        assertNull(Formatters.guessDayNumber("PTT-20241012-WA0003.opus"))
        assertNull(Formatters.guessDayNumber(null))
    }

    @Test
    fun speedLabels() {
        assertEquals("1x", Formatters.speedLabel(1.0f))
        assertEquals("1.25x", Formatters.speedLabel(1.25f))
        assertEquals("0.5x", Formatters.speedLabel(0.5f))
    }

    @Test
    fun safeFileNames() {
        assertEquals("a_b_c", Formatters.safeFileName("a/b:c"))
        assertEquals("file", Formatters.safeFileName("   "))
    }
}
