package com.englishpartner.hub

import com.englishpartner.hub.ui.viewer.ArticleParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleParserTest {

    @Test
    fun splitsOnBlankLines() {
        val p = ArticleParser.paragraphs("First line\ncontinues.\n\nSecond paragraph.\r\n\r\n\r\nThird.")
        assertEquals(listOf("First line\ncontinues.", "Second paragraph.", "Third."), p)
    }

    @Test
    fun splitsSingleNewlinesWhenNoBlankLines() {
        val p = ArticleParser.paragraphs("Hello\nHow are you?\n  \nFine")
        // "  " line is whitespace only, so it counts as a blank separator here.
        assertEquals(listOf("Hello\nHow are you?", "Fine"), p)
        val q = ArticleParser.paragraphs("One\nTwo\nThree")
        assertEquals(listOf("One", "Two", "Three"), q)
    }

    @Test
    fun emptyTextGivesNoParagraphs() {
        assertEquals(emptyList<String>(), ArticleParser.paragraphs("  \n \n"))
    }
}
