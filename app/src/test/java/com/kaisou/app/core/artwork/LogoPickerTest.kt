package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the choice of the logos of a title
class LogoPickerTest {
    // Builds a logo
    private fun logo(url: String, language: String?, width: Int = 1000, height: Int = 300, score: Double = 0.0) =
        Artwork(url, width, height, language, score)

    // The language of the app comes first, then English, then none
    @Test
    fun theLanguageOfTheAppComesFirstThenEnglishThenNone() {
        val picked = LogoPicker.pick(
            listOf(logo("ja", "ja"), logo("none", null), logo("en", "en"), logo("pt", "pt")),
            "pt"
        )
        assertEquals(listOf("pt", "en", "none"), picked)
    }

    // The small ones and the vector ones are left out
    @Test
    fun theSmallOnesAndTheVectorOnesAreLeftOut() {
        val picked = LogoPicker.pick(
            listOf(logo("small", "en", width = 300), logo("vector.svg", "en"), logo("ok", "en")),
            "en"
        )
        assertEquals(listOf("ok"), picked)
    }

    // In a language the biggest comes first and the rating settles a tie
    @Test
    fun inALanguageTheBiggestComesFirstAndTheRatingSettlesATie() {
        val picked = LogoPicker.pick(
            listOf(logo("a", "en", width = 1000), logo("b", "en", width = 2000, height = 600), logo("c", "en", width = 1000, score = 5.0)),
            "en"
        )
        assertEquals(listOf("b", "c", "a"), picked)
    }

    // The ones taller than wide are left out
    @Test
    fun theOnesTallerThanWideAreLeftOut() {
        val picked = LogoPicker.pick(listOf(logo("tall", "en", width = 1000, height = 1400), logo("wide", "en")), "en")
        assertEquals(listOf("wide"), picked)
    }

    // Only a few are kept
    @Test
    fun onlyAFewAreKept() {
        val picked = LogoPicker.pick((1..8).map { logo("l$it", "en", width = 1000 + it) }, "en")
        assertEquals(LogoPicker.MaxCandidates, picked.size)
    }

    // A language outside the list still beats nothing
    @Test
    fun aLanguageOutsideTheListStillBeatsNothing() {
        assertEquals(listOf("ja"), LogoPicker.pick(listOf(logo("ja", "ja")), "pt"))
    }
}
