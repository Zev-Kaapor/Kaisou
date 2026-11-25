package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests of the themes and the news of a title
class ThemeAndNewsTest {
    // The openings come before the endings with their songs and episodes
    @Test
    fun theOpeningsComeBeforeTheEndingsWithTheirSongsAndEpisodes() {
        val body = """{"data":{"theme":{
            "openings":["1: \"Yuusha (勇者)\" by YOASOBI (eps 1-16)","2: \"Haru (晴る)\" by Yorushika (ヨルシカ) (eps 17-28)"],
            "endings":["1: \"Anytime Anywhere\" by milet (eps 1-14,16-28)","S1: \"bliss\" by milet (eps Special Broadcast: 1-4)"]}}}"""
        val themes = ThemeClient.parse(body)
        assertEquals(listOf("OP1", "OP2", "ED1", "EDS1"), themes.map { it.label })
        assertEquals("Yuusha (勇者)", themes[0].song)
        assertEquals("YOASOBI", themes[0].artist)
        assertEquals("1-16", themes[0].episodes)
        assertEquals("Yorushika (ヨルシカ)", themes[1].artist)
    }

    // A line that does not follow the form is kept whole
    @Test
    fun aLineThatDoesNotFollowTheFormIsKeptWhole() {
        val themes = ThemeClient.parse("""{"data":{"theme":{"openings":["Some song"],"endings":[]}}}""")
        assertEquals(1, themes.size)
        assertEquals("OP", themes[0].label)
        assertEquals("Some song", themes[0].song)
        assertNull(themes[0].artist)
    }

    // A title without themes has none
    @Test
    fun aTitleWithoutThemesHasNone() {
        assertTrue(ThemeClient.parse("{\"data\":{}}").isEmpty())
    }

    // The news of an anime come newest first with their marks undone
    @Test
    fun theNewsOfAnAnimeComeNewestFirstWithTheirMarksUndone() {
        val body = """{"news":[
            {"title":"&quot;Frieren&quot; reveals a visual","link":"https://example.org/a","time":100},
            {"title":"Newer &amp; better","link":".123456","time":200},
            {"title":"No address","link":"","time":300}]}"""
        val news = NewsClient.parseKuroiru(body)
        assertEquals(listOf("Newer & better", "\"Frieren\" reveals a visual"), news.map { it.title })
        assertEquals("https://www.animenewsnetwork.com/news/.123456", news[0].url)
    }
}
