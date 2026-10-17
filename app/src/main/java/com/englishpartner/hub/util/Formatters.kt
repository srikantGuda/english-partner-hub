package com.englishpartner.hub.util

import java.util.Locale

/** Small pure helpers, kept free of Android classes so they are unit-testable. */
object Formatters {

    /** 65_000 ms -> "1:05", 3_725_000 ms -> "1:02:05". */
    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun fileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    /** Percentage 0..100, safe for a zero total. */
    fun percent(done: Int, total: Int): Int =
        if (total <= 0) 0 else ((done.coerceIn(0, total) * 100f) / total).toInt()

    /**
     * Try to find a day number in a file name, e.g. "Day 12 audio.mp3", "day_07.pdf", "D-3 notes".
     * Returns null when nothing sensible is found.
     */
    fun guessDayNumber(fileName: String?): Int? {
        if (fileName.isNullOrBlank()) return null
        val regex = Regex("""(?i)(?<![a-z])(?:day|d)[\s_\-.]*0*(\d{1,3})(?!\d)""")
        val match = regex.find(fileName) ?: return null
        val n = match.groupValues[1].toIntOrNull() ?: return null
        return if (n in 1..999) n else null
    }

    /** Make a name safe to use as a file name inside app storage. */
    fun safeFileName(name: String): String {
        val cleaned = name.replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), "_").trim()
        return cleaned.ifEmpty { "file" }.take(120)
    }

    val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    fun speedLabel(speed: Float): String {
        val text = if (speed % 1f == 0f) String.format(Locale.US, "%.0f", speed)
        else String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
        return "${text}x"
    }
}
