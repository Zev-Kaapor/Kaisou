package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ForumLinksTest {
    @Test
    fun findsTheTitlesOfTheLinksThatStandAlone() {
        val text = "first\n\nhttps://anilist.co/anime/20920/Name/\n\nhttps://anilist.co/manga/30013\ntext https://anilist.co/anime/5/x in a line\nhttps://anilist.co/anime/20920/Name/"
        assertEquals(listOf(20920, 30013), ForumLinks.ids(text))
    }

    @Test
    fun cutsTheTextWhereAKnownLinkIs() {
        val text = "a\n\nhttps://anilist.co/anime/20920/Name/\n\nb\nc"
        assertEquals(listOf(ForumPart.Text("a"), ForumPart.Card(20920), ForumPart.Text("b\nc")), ForumLinks.split(text, setOf(20920)))
    }

    @Test
    fun aLinkToAnUnknownTitleStaysInTheText() {
        val text = "a\nhttps://anilist.co/anime/7/x\nb"
        assertEquals(listOf(ForumPart.Text(text)), ForumLinks.split(text, emptySet()))
    }
}
