package com.kaisou.app.features.home.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusMarkdownTest {
    @Test
    fun aTextWithAPictureAndAVideoIsCutInTheirPlaces() {
        val blocks = StatusMarkdown.blocks("before img220(https://x/a.png) middle\nyoutube(https://y/b) webm(https://z/c.webm) after")
        assertEquals(
            listOf(
                MarkdownBlock.Text("before"),
                MarkdownBlock.Image("https://x/a.png"),
                MarkdownBlock.Text("middle"),
                MarkdownBlock.Video("https://y/b", youtube = true),
                MarkdownBlock.Video("https://z/c.webm", youtube = false),
                MarkdownBlock.Text("after")
            ),
            blocks
        )
    }

    @Test
    fun aTextWithNoPictureIsOnePartWithItsLinesKept() {
        assertEquals(listOf<MarkdownBlock>(MarkdownBlock.Text("one\ntwo")), StatusMarkdown.blocks("one\ntwo"))
    }

    @Test
    fun aFormatWrapsTheSelectionAndPutsTheCursorInsideWhenNothingIsSelected() {
        val selected = StatusMarkdown.apply(TextFieldValue("a word here", TextRange(2, 6)), MarkdownFormat.Bold)
        assertEquals("a **word** here", selected.text)
        val empty = StatusMarkdown.apply(TextFieldValue("ab", TextRange(1)), MarkdownFormat.Bold)
        assertEquals("a****b", empty.text)
        assertEquals(3, empty.selection.start)
    }

    @Test
    fun theListsNumberAndMarkEachLineOfTheSelection() {
        val numbered = StatusMarkdown.apply(TextFieldValue("x\ny", TextRange(0, 3)), MarkdownFormat.OrderedList)
        assertEquals("1. x\n2. y", numbered.text)
        val bulleted = StatusMarkdown.apply(TextFieldValue("x\ny", TextRange(0, 3)), MarkdownFormat.UnorderedList)
        assertEquals("- x\n- y", bulleted.text)
    }

    @Test
    fun aFormatThatAsksForAnAddressPutsItInTheSyntax() {
        val linked = StatusMarkdown.apply(TextFieldValue("", TextRange(0)), MarkdownFormat.Link, "https://anilist.co")
        assertEquals("[link](https://anilist.co)", linked.text)
    }
}
