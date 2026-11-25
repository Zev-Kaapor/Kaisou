package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the scores of the episodes
class EpisodeRatingsTest {
    // Builds an episode with the day it aired
    private fun episode(number: Int, aired: String?) = TitleEpisode(number, null, null, null, aired)

    // Each episode takes the first source that has it
    @Test
    fun eachEpisodeTakesTheFirstSourceThatHasIt() {
        val merged = EpisodeRatings.merge(
            listOf(
                mapOf(1 to 8.1f),
                mapOf(1 to 6.0f, 2 to 7.4f),
                mapOf(2 to 5.0f, 3 to 7.8f)
            )
        )
        assertEquals(mapOf(1 to 8.1f, 2 to 7.4f, 3 to 7.8f), merged)
    }

    // An episode no source scored has none
    @Test
    fun anEpisodeNoSourceScoredHasNone() {
        assertNull(EpisodeRatings.merge(listOf(emptyMap(), mapOf(1 to 7f)))[2])
    }

    // The scores of MyAnimeList go from five to ten
    @Test
    fun theScoresOfMyAnimeListGoFromFiveToTen() {
        val answer = EpisodeRatings.parseMal(
            """{"pagination":{"has_next_page":true},"data":[{"mal_id":1,"score":3.72},{"mal_id":2,"score":null},{"mal_id":3,"score":0}]}"""
        )
        assertEquals(1, answer?.first?.size)
        assertEquals(7.44f, answer?.first?.get(1) ?: 0f, 0.001f)
        assertEquals(true, answer?.second)
    }

    // An answer that is not one of episodes is left out
    @Test
    fun anAnswerThatIsNotOneOfEpisodesIsLeftOut() {
        assertNull(EpisodeRatings.parseMal("""{"error":"x"}"""))
    }

    // The day pairs episodes whose seasons are not the same
    @Test
    fun theDayPairsEpisodesWhoseSeasonsAreNotTheSame() {
        val episodes = listOf(episode(1, "2017-10-03"), episode(2, "2017-10-10"), episode(3, null))
        val imdb = listOf(ImdbClient.Episode(2, 5, "2017-10-04", 8f), ImdbClient.Episode(1, 1, "2017-10-10", 7f))
        val pairs = DayMatcher.pair(episodes, imdb, day = { it.released }, order = { it.season * 10_000 + it.number })
        assertEquals(8f, pairs[1]?.rating ?: 0f, 0.001f)
        assertEquals(7f, pairs[2]?.rating ?: 0f, 0.001f)
        assertNull(pairs[3])
    }

    // Two episodes on the same day are given in order
    @Test
    fun twoEpisodesOnTheSameDayAreGivenInOrder() {
        val episodes = listOf(episode(1, "2020-01-01"), episode(2, "2020-01-01"))
        val items = listOf(ImdbClient.Episode(1, 2, "2020-01-01", 9f), ImdbClient.Episode(1, 1, "2020-01-01", 6f))
        val pairs = DayMatcher.pair(episodes, items, day = { it.released }, order = { it.number })
        assertEquals(6f, pairs[1]?.rating ?: 0f, 0.001f)
        assertEquals(9f, pairs[2]?.rating ?: 0f, 0.001f)
    }

    // Episodes that do not match one to one are left out
    @Test
    fun episodesThatDoNotMatchOneToOneAreLeftOut() {
        val episodes = listOf(episode(1, "2020-01-01"), episode(2, "2020-01-01"))
        val items = listOf(ImdbClient.Episode(1, 1, "2020-01-01", 6f), ImdbClient.Episode(1, 2, "2020-01-01", 7f), ImdbClient.Episode(1, 3, "2020-01-01", 8f))
        assertEquals(emptyMap<Int, ImdbClient.Episode>(), DayMatcher.pair(episodes, items, day = { it.released }, order = { it.number }))
    }
}
