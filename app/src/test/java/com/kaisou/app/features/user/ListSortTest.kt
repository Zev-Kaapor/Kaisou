package com.kaisou.app.features.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.kaisou.app.features.user.data.ListSort
import com.kaisou.app.features.user.data.UserListEntry

// Tests of the order of the lists: the way each one goes, turned round, and what has nothing to compare staying last
class ListSortTest {
    // A title with only what the tests change
    private fun entry(id: Int, title: String, score: Int = 0, popularity: Int = 0) = UserListEntry(
        mediaId = id, title = title, romaji = title, cover = null, format = null, formatKey = null, source = null, airing = null, season = null,
        country = null, tags = emptyList(), total = null, progress = 0, score = score, status = "", updatedAt = 0, addedAt = 0, startedOn = 0,
        completedOn = 0, releaseOn = 0, popularity = popularity, year = null, genres = emptyList(), average = 0, malId = null, adult = false,
        manga = false, isMovie = false
    )

    // Three titles, one of them with no score
    private val list = listOf(entry(1, "Bleach", score = 70, popularity = 5), entry(2, "Akira", score = 90), entry(3, "Naruto", score = 0, popularity = 9))

    // The title goes up from A, and turned round goes down from Z
    @Test
    fun theTitleGoesUpFromAAndTurnedRoundGoesDownFromZ() {
        assertEquals(listOf("Akira", "Bleach", "Naruto"), list.sortedWith(ListSort.Title.comparator()).map { it.title })
        assertEquals(listOf("Naruto", "Bleach", "Akira"), list.sortedWith(ListSort.Title.comparator(reversed = true)).map { it.title })
    }

    // The score goes down from the highest, and turned round goes up from the lowest
    @Test
    fun theScoreGoesDownFromTheHighestAndTurnedRoundGoesUp() {
        assertEquals(listOf("Akira", "Bleach"), list.sortedWith(ListSort.Score.comparator()).map { it.title }.take(2))
        assertEquals(listOf("Bleach", "Akira"), list.sortedWith(ListSort.Score.comparator(reversed = true)).map { it.title }.take(2))
    }

    // A title with no score stays last whichever way the order goes
    @Test
    fun aTitleWithNoScoreStaysLastWhicheverWayItGoes() {
        assertEquals("Naruto", list.sortedWith(ListSort.Score.comparator()).last().title)
        assertEquals("Naruto", list.sortedWith(ListSort.Score.comparator(reversed = true)).last().title)
    }

    // The titles that are tied come in the order of their names
    @Test
    fun theTitlesThatAreTiedComeInTheOrderOfTheirNames() {
        assertEquals(listOf("Akira", "Bleach", "Naruto"), list.sortedWith(ListSort.Release.comparator()).map { it.title })
    }

    // Only the title goes up by itself, and turning round swaps the way of each
    @Test
    fun onlyTheTitleGoesUpByItselfAndTurningRoundSwapsTheWayOfEach() {
        assertTrue(ListSort.Title.goesUp(false))
        assertFalse(ListSort.Title.goesUp(true))
        assertFalse(ListSort.Popularity.goesUp(false))
        assertTrue(ListSort.Popularity.goesUp(true))
    }
}
