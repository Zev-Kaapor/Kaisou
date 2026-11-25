package com.kaisou.app.core.rating

import com.kaisou.app.core.artwork.ArtQuery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test

// Tests of the rating of a card
class RatingRepositoryTest {
    // Builds the query of a manga
    private fun manga(id: Int, adult: Boolean) = RatingQuery(id, malId = null, adult = adult, manga = true, art = ArtQuery(listOf("Title"), null, false))

    private val original = RatingRepository.mangaRating

    // The age of a manga is asked of AniList, so the tests give it by hand and put the real one back
    @After
    fun putTheRealOneBack() {
        RatingRepository.mangaRating = original
    }

    // A manga with nothing to say it by is not rated
    @Test
    fun aMangaWithNothingToSayItByIsNotRated() {
        RatingRepository.mangaRating = { _, _ -> ContentRating.NotRated }
        val rating = runBlocking { RatingRepository.forCard("BR", manga(9_000_001, adult = false)) }!!
        assertEquals(Unrated.NotRated, rating.unrated)
        assertTrue(rating.official)
        assertFalse(rating.symbol)
    }

    // A manga takes the age worked out from its tags
    @Test
    fun aMangaTakesTheAgeWorkedOutFromItsTags() {
        RatingRepository.mangaRating = { _, region -> ContentRating(AgeGroup.Sixteen, official = false, symbol = region == "BR") }
        val rating = runBlocking { RatingRepository.forCard("BR", manga(9_000_004, adult = false)) }!!
        assertNull(rating.unrated)
        assertEquals(AgeGroup.Sixteen, rating.group)
        assertFalse(rating.official)
        assertTrue(rating.symbol)
    }

    // An adult manga is for eighteen when its tags could not be asked
    @Test
    fun anAdultMangaIsForEighteenWhenItsTagsCouldNotBeAsked() {
        RatingRepository.mangaRating = { _, _ -> null }
        val rating = runBlocking { RatingRepository.forCard("US", manga(9_000_002, adult = true)) }!!
        assertNull(rating.unrated)
        assertEquals(AgeGroup.Eighteen, rating.group)
    }

    // A manga whose tags could not be asked has nothing and is not kept
    @Test
    fun aMangaWhoseTagsCouldNotBeAskedHasNothingAndIsNotKept() {
        RatingRepository.mangaRating = { _, _ -> null }
        val query = manga(9_000_005, adult = false)
        assertNull(runBlocking { RatingRepository.forCard("BR", query) })
        assertFalse(RatingRepository.known("BR", query).first)
    }

    // What was found is known afterwards
    @Test
    fun whatWasFoundIsKnownAfterwards() {
        RatingRepository.mangaRating = { _, _ -> ContentRating.NotRated }
        val query = manga(9_000_003, adult = false)
        assertFalse(RatingRepository.known("BR", query).first)
        runBlocking { RatingRepository.forCard("BR", query) }
        val (isKnown, rating) = RatingRepository.known("BR", query)
        assertTrue(isKnown)
        assertEquals(Unrated.NotRated, rating!!.unrated)
    }
}
