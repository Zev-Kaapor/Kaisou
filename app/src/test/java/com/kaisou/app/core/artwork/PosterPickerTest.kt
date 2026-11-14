package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosterPickerTest {
    private fun poster(url: String, width: Int = 2000, height: Int = 3000, language: String? = null, score: Double = 0.0) =
        Artwork(url, width, height, language, score)

    @Test
    fun noLanguageBeatsTheLanguageOfTheApp() {
        val chosen = PosterPicker.pick(listOf(poster("pt", language = "pt"), poster("none")), "pt")
        assertEquals("none", chosen?.url)
    }

    @Test
    fun theLanguageOfTheAppBeatsEnglish() {
        val chosen = PosterPicker.pick(listOf(poster("en", language = "en"), poster("pt", language = "pt")), "pt")
        assertEquals("pt", chosen?.url)
    }

    @Test
    fun englishIsTheLastChoice() {
        val chosen = PosterPicker.pick(listOf(poster("en", language = "en"), poster("ja", language = "ja")), "pt")
        assertEquals("en", chosen?.url)
    }

    @Test
    fun theSharpestOfTheGroupWins() {
        val chosen = PosterPicker.pick(listOf(poster("small", 1200, 1800), poster("big", 2000, 3000), poster("mid", 1500, 2250)), "pt")
        assertEquals("big", chosen?.url)
    }

    @Test
    fun theSharperOneOfTheNextGroupDoesNotBeatTheBetterGroup() {
        val chosen = PosterPicker.pick(listOf(poster("none", 1200, 1800), poster("pt", 3000, 4500, language = "pt")), "pt")
        assertEquals("none", chosen?.url)
    }

    @Test
    fun theRatingSettlesATieInSize() {
        val chosen = PosterPicker.pick(listOf(poster("low", score = 3.0), poster("high", score = 7.0)), "pt")
        assertEquals("high", chosen?.url)
    }

    @Test
    fun smallPostersAreLeftOut() {
        assertNull(PosterPicker.pick(listOf(poster("small", 680, 1000)), "pt"))
    }

    @Test
    fun postersThatAreNotStandingUpAreLeftOut() {
        assertNull(PosterPicker.pick(listOf(poster("wide", 3000, 2000), poster("square", 2000, 2000)), "pt"))
    }

    @Test
    fun anotherLanguageIsTheLastResort() {
        val chosen = PosterPicker.pick(listOf(poster("ru", 1500, 2250, language = "ru"), poster("zh", 2000, 3000, language = "zh")), "pt")
        assertEquals("zh", chosen?.url)
    }

    @Test
    fun anotherLanguageNeverBeatsTheOnesBeforeIt() {
        val chosen = PosterPicker.pick(listOf(poster("ru", 3000, 4500, language = "ru"), poster("en", 1200, 1800, language = "en")), "pt")
        assertEquals("en", chosen?.url)
    }

    @Test
    fun anotherLanguageThatIsSmallIsStillLeftOut() {
        assertNull(PosterPicker.pick(listOf(poster("ru", 680, 1000, language = "ru")), "pt"))
    }

    @Test
    fun nothingGivesNothing() {
        assertNull(PosterPicker.pick(emptyList(), "pt"))
    }
}
