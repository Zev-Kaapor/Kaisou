package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the choice of the poster of a season
class PosterPickerSeasonTest {
    // Builds a poster
    private fun poster(url: String, width: Int, height: Int, language: String?) = Artwork(url, width, height, language, 0.0)

    // A season poster of the size of TheTVDB is kept when the limit is lowered
    @Test
    fun aSeasonPosterOfTheTvdbSizeIsKeptWhenTheLimitIsLowered() {
        val posters = listOf(poster("a", 680, 1000, "en"))
        assertNull(PosterPicker.pick(posters, "pt"))
        assertEquals("a", PosterPicker.pick(posters, "pt", PosterPicker.SeasonMinWidth)?.url)
    }

    // The language of the app comes before English among season posters
    @Test
    fun theLanguageOfTheAppComesBeforeEnglishAmongSeasonPosters() {
        val posters = listOf(poster("en", 680, 1000, "en"), poster("pt", 680, 1000, "pt"), poster("ja", 2000, 3000, "ja"))
        assertEquals("pt", PosterPicker.pick(posters, "pt", PosterPicker.SeasonMinWidth)?.url)
    }

    // A picture that is not standing up is left out
    @Test
    fun aPictureThatIsNotStandingUpIsLeftOut() {
        val posters = listOf(poster("wide", 1920, 1080, null))
        assertNull(PosterPicker.pick(posters, "pt", PosterPicker.SeasonMinWidth))
    }
}
