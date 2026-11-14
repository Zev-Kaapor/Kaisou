package com.kaisou.app.core.anilist

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonNumberTest {
    private val cyberpunk = mapOf(1 to LocalDate.of(2022, 9, 13), 2 to LocalDate.of(2026, 10, 20))
    private val revengers = mapOf(
        1 to LocalDate.of(2021, 4, 11),
        2 to LocalDate.of(2023, 1, 8),
        3 to LocalDate.of(2023, 10, 4),
        4 to LocalDate.of(2026, 10, 3)
    )

    @Test
    fun theSeasonThatAiredWhenTheTitleStartedIsTheOne() {
        assertEquals(2, SeasonNumber.byDate(LocalDate.of(2026, 10, 20), cyberpunk))
        assertEquals(4, SeasonNumber.byDate(LocalDate.of(2026, 10, 3), revengers))
        assertEquals(1, SeasonNumber.byDate(LocalDate.of(2022, 9, 13), cyberpunk))
    }

    @Test
    fun aFewDaysApartStillCounts() {
        assertEquals(2, SeasonNumber.byDate(LocalDate.of(2026, 10, 25), cyberpunk))
    }

    @Test
    fun aSeasonTooFarAwayDoesNotCount() {
        assertNull(SeasonNumber.byDate(LocalDate.of(2024, 5, 1), cyberpunk))
        assertNull(SeasonNumber.byDate(null, cyberpunk))
        assertNull(SeasonNumber.byDate(LocalDate.of(2026, 10, 3), emptyMap()))
    }

    @Test
    fun theNumberInTheNameIsTheSeasonWhenTheDatesDoNotSayIt() {
        assertEquals(2, SeasonNumber.marker(listOf("Koori no Jouheki 2nd Season")))
        assertEquals(3, SeasonNumber.marker(listOf("The Apothecary Diaries Season 3")))
        assertEquals(3, SeasonNumber.marker(listOf("Mushoku Tensei Third Season")))
        assertEquals(2, SeasonNumber.marker(listOf("Shingeki no Kyojin 2期")))
    }

    @Test
    fun theFirstNameThatSaysItIsTheOneUsed() {
        assertEquals(4, SeasonNumber.marker(listOf("Plain Name", "Plain Name 4th Season")))
    }

    @Test
    fun partsAndArcsAreNotSeasons() {
        assertNull(SeasonNumber.marker(listOf("Tokyo Revengers: Santen Sensou-hen")))
        assertNull(SeasonNumber.marker(listOf("Something Part 2", "Final Season")))
    }

    @Test
    fun theOrderIsDatesThenNameThenFirstWhenNothingCameBefore() {
        assertEquals(4, SeasonNumber.resolve(byDate = 4, marker = 3, hasPrequel = true))
        assertEquals(3, SeasonNumber.resolve(byDate = null, marker = 3, hasPrequel = true))
        assertEquals(1, SeasonNumber.resolve(byDate = null, marker = null, hasPrequel = false))
        assertNull(SeasonNumber.resolve(byDate = null, marker = null, hasPrequel = true))
    }
}
