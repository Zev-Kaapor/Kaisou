package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the reading of the answer of IMDb
class ImdbClientTest {
    // What the open door of IMDb answers for the episodes of a series, as far as the parts that are read
    private val answer = """{"data":{"title":{"episodes":{"episodes":{
        "pageInfo":{"hasNextPage":true,"endCursor":"abc"},
        "edges":[
            {"node":{"releaseDate":{"year":2017,"month":10,"day":3},"ratingsSummary":{"aggregateRating":7.6,"voteCount":4676},"series":{"episodeNumber":{"episodeNumber":1,"seasonNumber":1}}}},
            {"node":{"releaseDate":{"year":2026,"month":11,"day":28},"ratingsSummary":{"aggregateRating":null,"voteCount":0},"series":{"episodeNumber":{"episodeNumber":9,"seasonNumber":5}}}},
            {"node":{"releaseDate":null,"ratingsSummary":{"aggregateRating":9.9,"voteCount":2},"series":{"episodeNumber":{"episodeNumber":3,"seasonNumber":1}}}},
            {"node":{"releaseDate":null,"ratingsSummary":{"aggregateRating":8.4,"voteCount":120},"series":{"episodeNumber":{"episodeNumber":4,"seasonNumber":1}}}}
        ]}}}}}"""

    // The episodes with a score from enough people are kept
    @Test
    fun theEpisodesWithAScoreFromEnoughPeopleAreKept() {
        val page = ImdbClient.parsePage(answer)
        assertEquals(listOf(1 to 1, 1 to 4), page?.episodes?.map { it.season to it.number })
        assertEquals(7.6f, page?.episodes?.first()?.rating ?: 0f, 0.001f)
    }

    // The day comes from the year, the month and the day
    @Test
    fun theDayComesFromTheYearTheMonthAndTheDay() {
        val page = ImdbClient.parsePage(answer)
        assertEquals("2017-10-03", page?.episodes?.first()?.released)
        assertNull(page?.episodes?.last()?.released)
    }

    // The page says whether there is another and where it begins
    @Test
    fun thePageSaysWhetherThereIsAnotherAndWhereItBegins() {
        val page = ImdbClient.parsePage(answer)
        assertEquals(true, page?.hasNext)
        assertEquals("abc", page?.cursor)
    }

    // The question asks for the page that the tested one did
    @Test
    fun theQuestionAsksForThePageThatTheTestedOneDid() {
        // The text that worked when it was tried by hand, with its variables, its page size and the fields that are read
        assertEquals(
            """query(${'$'}id:ID!,${'$'}after:ID){title(id:${'$'}id){episodes{episodes(first:250,after:${'$'}after){pageInfo{hasNextPage endCursor} edges{node{releaseDate{year month day} ratingsSummary{aggregateRating voteCount} series{episodeNumber{episodeNumber seasonNumber}}}}}}}}""",
            ImdbClient.Query
        )
    }

    // An answer that is not one of episodes is left out
    @Test
    fun anAnswerThatIsNotOneOfEpisodesIsLeftOut() {
        assertNull(ImdbClient.parsePage("""{"errors":[{"message":"x"}]}"""))
        assertNull(ImdbClient.parsePage("""{"data":{"title":null}}"""))
        assertNull(ImdbClient.parsePage("not json"))
    }
}
