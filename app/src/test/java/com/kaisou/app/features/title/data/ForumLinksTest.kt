package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the links of the forum
class ForumLinksTest {
    // Finds the titles of the links that stand alone
    @Test
    fun findsTheTitlesOfTheLinksThatStandAlone() {
        val text = "first\n\nhttps://anilist.co/anime/20920/Name/\n\nhttps://anilist.co/manga/30013\ntext https://anilist.co/anime/5/x in a line\nhttps://anilist.co/anime/20920/Name/"
        assertEquals(listOf(20920, 30013), ForumLinks.ids(text))
    }

    // Cuts the text where a known link is
    @Test
    fun cutsTheTextWhereAKnownLinkIs() {
        val text = "a\n\nhttps://anilist.co/anime/20920/Name/\n\nb\nc"
        assertEquals(listOf(ForumPart.Text("a"), ForumPart.Card(20920), ForumPart.Text("b\nc")), ForumLinks.split(text, setOf(20920)))
    }

    // A link to an unknown title stays in the text
    @Test
    fun aLinkToAnUnknownTitleStaysInTheText() {
        val text = "a\nhttps://anilist.co/anime/7/x\nb"
        assertEquals(listOf(ForumPart.Text(text)), ForumLinks.split(text, emptySet()))
    }
}
