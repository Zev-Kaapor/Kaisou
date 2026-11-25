package com.kaisou.app.core.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests of the choice of the rating to show
class RatingPickerTest {
    private val certified = mapOf("BR" to "14", "US" to "TV-MA", "DE" to "12")

    // The Brazilian one shows the symbol
    @Test
    fun theBrazilianOneShowsTheSymbol() {
        val rating = RatingPicker.pick("BR", certified, MalRating.General, adult = false)!!
        assertEquals(AgeGroup.Fourteen, rating.group)
        assertTrue(rating.symbol)
        assertTrue(rating.official)
    }

    // Another country shows the global square
    @Test
    fun anotherCountryShowsTheGlobalSquare() {
        val rating = RatingPicker.pick("US", certified, null, adult = false)!!
        assertEquals(AgeGroup.Sixteen, rating.group)
        assertFalse(rating.symbol)
        assertTrue(rating.official)
    }

    // Without the country the rating of MyAnimeList is an approximation
    @Test
    fun withoutTheCountryTheRatingOfMyAnimeListIsAnApproximation() {
        val rating = RatingPicker.pick("FR", certified, MalRating.Children, adult = false)!!
        assertEquals(AgeGroup.Ten, rating.group)
        assertFalse(rating.official)
        assertFalse(rating.symbol)
    }

    // In Brazil an approximation still uses the symbol
    @Test
    fun inBrazilAnApproximationStillUsesTheSymbol() {
        val rating = RatingPicker.pick("BR", emptyMap(), MalRating.Teens, adult = false)!!
        assertEquals(AgeGroup.Fourteen, rating.group)
        assertTrue(rating.symbol)
        assertFalse(rating.official)
    }

    // With neither, the rating of another country serves
    @Test
    fun withNeitherTheRatingOfAnotherCountryServes() {
        val rating = RatingPicker.pick("FR", mapOf("DE" to "16"), null, adult = false)!!
        assertEquals(AgeGroup.Sixteen, rating.group)
        assertFalse(rating.official)
    }

    // A label that is not known falls to the next source
    @Test
    fun aLabelThatIsNotKnownFallsToTheNextSource() {
        val rating = RatingPicker.pick("US", mapOf("US" to "NR"), MalRating.Violence, adult = false)!!
        assertEquals(AgeGroup.Sixteen, rating.group)
        assertFalse(rating.official)
    }

    // The adult mark is the last resort
    @Test
    fun theAdultMarkIsTheLastResort() {
        assertEquals(AgeGroup.Eighteen, RatingPicker.pick("US", emptyMap(), null, adult = true)!!.group)
        assertNull(RatingPicker.pick("US", emptyMap(), null, adult = false))
    }

    // No country at all still works
    @Test
    fun noCountryAtAllStillWorks() {
        assertEquals(AgeGroup.Ten, RatingPicker.pick("", emptyMap(), MalRating.Children, adult = false)!!.group)
    }
}
