package com.kaisou.app.core.anilist

// Java time
import java.time.LocalDate

// A season of AniList with its year, which is how the titles that started airing at the same time are grouped
data class AniSeason(val name: String, val year: Int) {

    // The season that comes after this one, winter takes the year after fall
    fun next(): AniSeason = when (name) {
        Winter -> AniSeason(Spring, year)
        Spring -> AniSeason(Summer, year)
        Summer -> AniSeason(Fall, year)
        else -> AniSeason(Winter, year + 1)
    }

    // Gives the season of a day and holds the names AniList uses
    companion object {
        // Names of the seasons as AniList writes them
        private const val Winter = "WINTER"
        private const val Spring = "SPRING"
        private const val Summer = "SUMMER"
        private const val Fall = "FALL"

        // The season of a day as AniList counts it, December already belonging to the winter of the next year
        fun of(date: LocalDate): AniSeason = when (date.monthValue) {
            12 -> AniSeason(Winter, date.year + 1)
            1, 2 -> AniSeason(Winter, date.year)
            in 3..5 -> AniSeason(Spring, date.year)
            in 6..8 -> AniSeason(Summer, date.year)
            else -> AniSeason(Fall, date.year)
        }
    }
}
