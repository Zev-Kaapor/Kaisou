package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the grouping of the episodes in seasons or ranges
class EpisodeGroupsTest {
    // Builds a list of episodes, with the season of each
    private fun episodes(count: Int, season: (Int) -> Int? = { null }) =
        (1..count).map { TitleEpisode(it, null, null, null, null, season = season(it)) }

    // A short series is not cut
    @Test
    fun `a short series is not cut`() {
        assertEquals(1, EpisodeGroups.of(episodes(13)).size)
        assertEquals(1, EpisodeGroups.of(episodes(25)).size)
    }

    // Up to 249 episodes the ranges have 25
    @Test
    fun `up to 249 episodes the ranges have 25`() {
        val groups = EpisodeGroups.of(episodes(30))
        assertEquals(listOf(1 to 25, 26 to 30), groups.map { it.first to it.last })
        assertEquals(10, EpisodeGroups.of(episodes(250 - 1)).size)
    }

    // Up to 499 episodes the ranges have 50 and after that 100
    @Test
    fun `up to 499 episodes the ranges have 50 and after that 100`() {
        assertEquals(5, EpisodeGroups.of(episodes(250)).size)
        assertEquals(50, EpisodeGroups.of(episodes(250)).first().episodes.size)
        assertEquals(6, EpisodeGroups.of(episodes(600)).size)
        assertEquals(100, EpisodeGroups.of(episodes(600)).first().episodes.size)
    }

    // Two or more seasons are the groups
    @Test
    fun `two or more seasons are the groups`() {
        val groups = EpisodeGroups.of(episodes(60) { if (it <= 24) 1 else 2 })
        assertEquals(listOf(1, 2), groups.map { it.season })
        assertEquals(24, groups.first().episodes.size)
    }

    // The open group is the one picked, otherwise the one of the next episode
    @Test
    fun `the open group is the one picked, otherwise the one of the next episode`() {
        val groups = EpisodeGroups.of(episodes(80))
        assertEquals(1, EpisodeGroups.open(groups, picked = null, next = 30))
        assertEquals(0, EpisodeGroups.open(groups, picked = 0, next = 30))
        assertEquals(0, EpisodeGroups.open(groups, picked = null, next = null))
    }
}
