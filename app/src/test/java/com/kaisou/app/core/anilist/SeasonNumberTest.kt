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

    @Test
    fun theSecondPartOfASeasonGoesOnInThatSeason() {
        val chitose = mapOf(1 to LocalDate.of(2025, 10, 7))
        assertEquals(true, SeasonNumber.isContinuation(listOf("Chitose Is in the Ramune Bottle 2nd Cour")))
        assertEquals(true, SeasonNumber.isContinuation(listOf("Chitose-kun wa Ramune Bin no Naka Part 2")))
        assertEquals(false, SeasonNumber.isContinuation(listOf("Chitose-kun wa Ramune Bin no Naka")))
        assertEquals(1, SeasonNumber.of(LocalDate.of(2026, 10, 13), chitose, null, hasPrequel = true, continuation = true))
        assertEquals(4, SeasonNumber.of(LocalDate.of(2022, 1, 10), revengers + (4 to LocalDate.of(2020, 12, 7)) - 1, null, hasPrequel = true, continuation = true))
    }

    @Test
    fun aSecondPartIsNotASeasonWhenThereIsNoPartInTheName() {
        assertNull(SeasonNumber.of(LocalDate.of(2026, 10, 13), mapOf(1 to LocalDate.of(2025, 10, 7)), null, hasPrequel = true, continuation = false))
    }

    @Test
    fun aLaterStageOfASeasonGoesOnInIt() {
        val jojo = mapOf(5 to LocalDate.of(2022, 1, 8), 6 to LocalDate.of(2026, 3, 19))
        assertEquals(true, SeasonNumber.isContinuation(listOf("JoJo no Kimyou na Bouken: Steel Ball Run - 2nd & 3rd STAGE")))
        assertEquals(false, SeasonNumber.isContinuation(listOf("JoJo no Kimyou na Bouken: Steel Ball Run - 1st STAGE")))
        assertEquals(6, SeasonNumber.of(LocalDate.of(2026, 9, 25), jojo, null, hasPrequel = true, continuation = true))
    }

    @Test
    fun aTitleBetweenTwoSeasonsIsInTheFirstOfThem() {
        val series = mapOf(1 to LocalDate.of(2020, 1, 1), 2 to LocalDate.of(2023, 1, 1))
        assertEquals(1, SeasonNumber.of(LocalDate.of(2021, 6, 1), series, null, hasPrequel = true, continuation = false))
        assertNull(SeasonNumber.of(LocalDate.of(2025, 6, 1), series, null, hasPrequel = true, continuation = false))
    }
}
