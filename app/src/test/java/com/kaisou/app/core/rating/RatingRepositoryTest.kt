package com.kaisou.app.core.rating

import com.kaisou.app.core.artwork.ArtQuery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingRepositoryTest {
    private fun manga(id: Int, adult: Boolean) = RatingQuery(id, malId = null, adult = adult, manga = true, art = ArtQuery(listOf("Title"), null, false))

    @Test
    fun aMangaWithNoRatingIsNotRated() {
        val rating = runBlocking { RatingRepository.forCard("BR", manga(9_000_001, adult = false)) }!!
        assertEquals(Unrated.NotRated, rating.unrated)
        assertTrue(rating.official)
        assertFalse(rating.symbol)
    }

    @Test
    fun anAdultMangaIsForEighteen() {
        val rating = runBlocking { RatingRepository.forCard("US", manga(9_000_002, adult = true)) }!!
        assertNull(rating.unrated)
        assertEquals(AgeGroup.Eighteen, rating.group)
    }

    @Test
    fun whatWasFoundIsKnownAfterwards() {
        val query = manga(9_000_003, adult = false)
        assertFalse(RatingRepository.known("BR", query).first)
        runBlocking { RatingRepository.forCard("BR", query) }
        val (isKnown, rating) = RatingRepository.known("BR", query)
        assertTrue(isKnown)
        assertEquals(Unrated.NotRated, rating!!.unrated)
    }
}
