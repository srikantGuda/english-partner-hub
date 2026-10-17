package com.englishpartner.hub

import com.englishpartner.hub.data.model.MaterialType
import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialTypeTest {

    @Test
    fun detectsFromMimeType() {
        assertEquals(MaterialType.AUDIO, MaterialType.detect("audio/ogg; codecs=opus", "x"))
        assertEquals(MaterialType.AUDIO, MaterialType.detect("application/ogg", null))
        assertEquals(MaterialType.VIDEO, MaterialType.detect("video/mp4", null))
        assertEquals(MaterialType.PDF, MaterialType.detect("application/pdf", null))
        assertEquals(MaterialType.IMAGE, MaterialType.detect("image/jpeg", null))
        assertEquals(MaterialType.TEXT, MaterialType.detect("text/plain", null))
    }

    @Test
    fun fallsBackToExtension() {
        assertEquals(MaterialType.AUDIO, MaterialType.detect("application/octet-stream", "PTT-20241012-WA0003.opus"))
        assertEquals(MaterialType.VIDEO, MaterialType.detect(null, "lesson.MKV"))
        assertEquals(MaterialType.PDF, MaterialType.detect("", "Day 5 notes.pdf"))
        assertEquals(MaterialType.TEXT, MaterialType.detect(null, "chat.txt"))
        assertEquals(MaterialType.OTHER, MaterialType.detect(null, "archive.rar"))
        assertEquals(MaterialType.OTHER, MaterialType.detect(null, null))
    }

    @Test
    fun extensionParsing() {
        assertEquals("mp3", MaterialType.extensionOf("a.b.MP3"))
        assertEquals("", MaterialType.extensionOf("noext"))
        assertEquals("", MaterialType.extensionOf("trailingdot."))
    }

    @Test
    fun fromNameIsSafe() {
        assertEquals(MaterialType.PDF, MaterialType.fromName("PDF"))
        assertEquals(MaterialType.OTHER, MaterialType.fromName("nonsense"))
        assertEquals(MaterialType.OTHER, MaterialType.fromName(null))
    }
}
