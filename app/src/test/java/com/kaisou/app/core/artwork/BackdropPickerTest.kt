package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the choice of the backdrop of a title
class BackdropPickerTest {
    // Builds a backdrop
    private fun backdrop(url: String, language: String?, width: Int = 1920, height: Int = 1080, score: Double = 0.0) =
        Artwork(url, width, height, language, score)

    // The ones with no language come first
    @Test
    fun theOnesWithNoLanguageComeFirst() {
        val picked = BackdropPicker.pick(listOf(backdrop("en", "en"), backdrop("clean", null), backdrop("pt", "pt")), "pt")
        assertEquals("clean", picked?.url)
    }

    // With no clean one the language of the app, then English
    @Test
    fun withNoCleanOneTheLanguageOfTheAppThenEnglish() {
        assertEquals("pt", BackdropPicker.pick(listOf(backdrop("en", "en"), backdrop("pt", "pt")), "pt")?.url)
        assertEquals("en", BackdropPicker.pick(listOf(backdrop("ja", "ja"), backdrop("en", "en")), "pt")?.url)
    }

    // Any other language is the last resort
    @Test
    fun anyOtherLanguageIsTheLastResort() {
        assertEquals("ja", BackdropPicker.pick(listOf(backdrop("ja", "ja")), "pt")?.url)
    }

    // The small ones and the ones that are not wide are left out
    @Test
    fun theSmallOnesAndTheOnesThatAreNotWideAreLeftOut() {
        val picked = BackdropPicker.pick(
            listOf(
                backdrop("small", null, width = 1280, height = 720),
                backdrop("banner", null, width = 3000, height = 500),
                backdrop("tall", null, width = 1920, height = 2800),
                backdrop("ok", null)
            ),
            "pt"
        )
        assertEquals("ok", picked?.url)
        assertNull(BackdropPicker.pick(listOf(backdrop("small", null, width = 1280, height = 720)), "pt"))
    }

    // In a group the one with most pixels wins and the rating settles a tie
    @Test
    fun inAGroupTheOneWithMostPixelsWinsAndTheRatingSettlesATie() {
        val picked = BackdropPicker.pick(
            listOf(
                backdrop("plain", null),
                backdrop("sharp", null, width = 3840, height = 2160),
                backdrop("rated", null, width = 3840, height = 2160, score = 5.0)
            ),
            "pt"
        )
        assertEquals("rated", picked?.url)
    }
}
