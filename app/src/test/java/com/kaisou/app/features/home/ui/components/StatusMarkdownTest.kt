package com.kaisou.app.features.home.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the rendering of the formats of a text of AniList
class StatusMarkdownTest {
    // A text with a picture and a video is cut in their places
    @Test
    fun aTextWithAPictureAndAVideoIsCutInTheirPlaces() {
        val blocks = StatusMarkdown.blocks("before img220(https://x/a.png) middle\nyoutube(https://y/b) webm(https://z/c.webm) after")
        assertEquals(
            listOf(
                MarkdownBlock.Text("before"),
                MarkdownBlock.Image("https://x/a.png", width = 220),
                MarkdownBlock.Text("middle"),
                MarkdownBlock.Video("https://y/b", youtube = true),
                MarkdownBlock.Video("https://z/c.webm", youtube = false),
                MarkdownBlock.Text("after")
            ),
            blocks
        )
    }

    // A text with no picture is one part with its lines kept
    @Test
    fun aTextWithNoPictureIsOnePartWithItsLinesKept() {
        assertEquals(listOf<MarkdownBlock>(MarkdownBlock.Text("one\ntwo")), StatusMarkdown.blocks("one\ntwo"))
    }

    // An italic that begins in a line and ends in the next one is resolved
    @Test
    fun anItalicThatBeginsInALineAndEndsInTheNextOneIsResolved() {
        val rendered = StatusMarkdown.render("_first line\nsecond line_\n\nplain _one_", Color.Blue, 16.sp)
        assertEquals("first line\nsecond line\n\nplain one", rendered.text)
        val italic = rendered.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }.map { it.start to it.end }
        assertEquals(listOf(0 to 22, 30 to 33), italic.sortedBy { it.first })
    }

    // A format wraps the selection and puts the cursor inside when nothing is selected
    @Test
    fun aFormatWrapsTheSelectionAndPutsTheCursorInsideWhenNothingIsSelected() {
        val selected = StatusMarkdown.apply(TextFieldValue("a word here", TextRange(2, 6)), MarkdownFormat.Bold)
        assertEquals("a **word** here", selected.text)
        val empty = StatusMarkdown.apply(TextFieldValue("ab", TextRange(1)), MarkdownFormat.Bold)
        assertEquals("a****b", empty.text)
        assertEquals(3, empty.selection.start)
    }

    // The lists number and mark each line of the selection
    @Test
    fun theListsNumberAndMarkEachLineOfTheSelection() {
        val numbered = StatusMarkdown.apply(TextFieldValue("x\ny", TextRange(0, 3)), MarkdownFormat.OrderedList)
        assertEquals("1. x\n2. y", numbered.text)
        val bulleted = StatusMarkdown.apply(TextFieldValue("x\ny", TextRange(0, 3)), MarkdownFormat.UnorderedList)
        assertEquals("- x\n- y", bulleted.text)
    }

    // A format that asks for an address puts it in the syntax
    @Test
    fun aFormatThatAsksForAnAddressPutsItInTheSyntax() {
        val linked = StatusMarkdown.apply(TextFieldValue("", TextRange(0)), MarkdownFormat.Link, "https://anilist.co")
        assertEquals("[link](https://anilist.co)", linked.text)
    }
}
