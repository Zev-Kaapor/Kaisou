package com.kaisou.app.features.home.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the cutting of a text of AniList in parts
class StatusMarkdownBlocksTest {
    // A centered picture stands alone and the caption stays centered
    @Test
    fun aCenteredPictureStandsAloneAndTheCaptionStaysCentered() {
        val text = "intro\n\n~~~img440(https://a.test/b.jpg)~~~\n~~~*caption*~~~\n\nnext"
        assertEquals(
            listOf(
                MarkdownBlock.Text("intro"),
                MarkdownBlock.Image("https://a.test/b.jpg", width = 440, centered = true),
                MarkdownBlock.Text("~~~*caption*~~~\n\nnext")
            ),
            StatusMarkdown.blocks(text)
        )
    }

    // A centered part with text around the picture keeps each pair of marks
    @Test
    fun aCenteredPartWithTextAroundThePictureKeepsEachPairOfMarks() {
        val text = "~~~before\nimg(https://a.test/b.jpg)\nafter~~~"
        assertEquals(
            listOf(
                MarkdownBlock.Text("~~~before~~~"),
                MarkdownBlock.Image("https://a.test/b.jpg", centered = true),
                MarkdownBlock.Text("~~~after~~~")
            ),
            StatusMarkdown.blocks(text)
        )
    }

    // The width of a picture is kept in pixels or in percent
    @Test
    fun theWidthOfAPictureIsKeptInPixelsOrInPercent() {
        assertEquals(
            listOf(MarkdownBlock.Image("https://a.test/b.jpg", width = 50, percent = true), MarkdownBlock.Image("https://a.test/c.jpg", width = 220)),
            StatusMarkdown.blocks("img50%(https://a.test/b.jpg)\n\nimg220(https://a.test/c.jpg)")
        )
    }

    // A text with no media is one part
    @Test
    fun aTextWithNoMediaIsOnePart() {
        assertEquals(listOf(MarkdownBlock.Text("one\n\ntwo\n~~~three~~~")), StatusMarkdown.blocks("one\n\ntwo\n~~~three~~~"))
    }
}
