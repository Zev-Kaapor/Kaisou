package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the choice of the poster of a title
class PosterPickerTest {
    // Builds a poster
    private fun poster(url: String, width: Int = 2000, height: Int = 3000, language: String? = null, score: Double = 0.0) =
        Artwork(url, width, height, language, score)

    // No language beats the language of the app
    @Test
    fun noLanguageBeatsTheLanguageOfTheApp() {
        val chosen = PosterPicker.pick(listOf(poster("pt", language = "pt"), poster("none")), "pt")
        assertEquals("none", chosen?.url)
    }

    // The language of the app beats English
    @Test
    fun theLanguageOfTheAppBeatsEnglish() {
        val chosen = PosterPicker.pick(listOf(poster("en", language = "en"), poster("pt", language = "pt")), "pt")
        assertEquals("pt", chosen?.url)
    }

    // English is the last choice
    @Test
    fun englishIsTheLastChoice() {
        val chosen = PosterPicker.pick(listOf(poster("en", language = "en"), poster("ja", language = "ja")), "pt")
        assertEquals("en", chosen?.url)
    }

    // The sharpest of the group wins
    @Test
    fun theSharpestOfTheGroupWins() {
        val chosen = PosterPicker.pick(listOf(poster("small", 1200, 1800), poster("big", 2000, 3000), poster("mid", 1500, 2250)), "pt")
        assertEquals("big", chosen?.url)
    }

    // The sharper one of the next group does not beat the better group
    @Test
    fun theSharperOneOfTheNextGroupDoesNotBeatTheBetterGroup() {
        val chosen = PosterPicker.pick(listOf(poster("none", 1200, 1800), poster("pt", 3000, 4500, language = "pt")), "pt")
        assertEquals("none", chosen?.url)
    }

    // The rating settles a tie in size
    @Test
    fun theRatingSettlesATieInSize() {
        val chosen = PosterPicker.pick(listOf(poster("low", score = 3.0), poster("high", score = 7.0)), "pt")
        assertEquals("high", chosen?.url)
    }

    // Small posters are left out
    @Test
    fun smallPostersAreLeftOut() {
        assertNull(PosterPicker.pick(listOf(poster("small", 680, 1000)), "pt"))
    }

    // Posters that are not standing up are left out
    @Test
    fun postersThatAreNotStandingUpAreLeftOut() {
        assertNull(PosterPicker.pick(listOf(poster("wide", 3000, 2000), poster("square", 2000, 2000)), "pt"))
    }

    // Another language is the last resort
    @Test
    fun anotherLanguageIsTheLastResort() {
        val chosen = PosterPicker.pick(listOf(poster("ru", 1500, 2250, language = "ru"), poster("zh", 2000, 3000, language = "zh")), "pt")
        assertEquals("zh", chosen?.url)
    }

    // Another language never beats the ones before it
    @Test
    fun anotherLanguageNeverBeatsTheOnesBeforeIt() {
        val chosen = PosterPicker.pick(listOf(poster("ru", 3000, 4500, language = "ru"), poster("en", 1200, 1800, language = "en")), "pt")
        assertEquals("en", chosen?.url)
    }

    // Another language that is small is still left out
    @Test
    fun anotherLanguageThatIsSmallIsStillLeftOut() {
        assertNull(PosterPicker.pick(listOf(poster("ru", 680, 1000, language = "ru")), "pt"))
    }

    // Nothing gives nothing
    @Test
    fun nothingGivesNothing() {
        assertNull(PosterPicker.pick(emptyList(), "pt"))
    }
}
