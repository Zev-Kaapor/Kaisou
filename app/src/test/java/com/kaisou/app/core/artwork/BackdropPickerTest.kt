package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackdropPickerTest {
    private fun backdrop(url: String, language: String?, width: Int = 1920, height: Int = 1080, score: Double = 0.0) =
        Artwork(url, width, height, language, score)

    @Test
    fun theOnesWithNoLanguageComeFirst() {
        val picked = BackdropPicker.pick(listOf(backdrop("en", "en"), backdrop("clean", null), backdrop("pt", "pt")), "pt")
        assertEquals("clean", picked?.url)
    }

    @Test
    fun withNoCleanOneTheLanguageOfTheAppThenEnglish() {
        assertEquals("pt", BackdropPicker.pick(listOf(backdrop("en", "en"), backdrop("pt", "pt")), "pt")?.url)
        assertEquals("en", BackdropPicker.pick(listOf(backdrop("ja", "ja"), backdrop("en", "en")), "pt")?.url)
    }

    @Test
    fun anyOtherLanguageIsTheLastResort() {
        assertEquals("ja", BackdropPicker.pick(listOf(backdrop("ja", "ja")), "pt")?.url)
    }

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
