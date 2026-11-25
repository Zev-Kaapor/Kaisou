package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the reading of the answers of TVmaze
class TvMazeClientTest {
    // The id of the series is the one of the answer
    @Test
    fun theIdOfTheSeriesIsTheOneOfTheAnswer() {
        assertEquals(31396, TvMazeClient.parseShow("""{"id":31396,"name":"Black Clover"}"""))
        assertNull(TvMazeClient.parseShow("""{"status":404}"""))
        assertNull(TvMazeClient.parseShow("not json"))
    }

    // The episodes with a number and a score are kept
    @Test
    fun theEpisodesWithANumberAndAScoreAreKept() {
        val episodes = TvMazeClient.parseEpisodes(
            """[
                {"season":1,"number":1,"airdate":"2017-10-03","rating":{"average":7.2}},
                {"season":1,"number":2,"airdate":"2017-10-10","rating":{"average":null}},
                {"season":0,"number":null,"airdate":"2016-11-27","rating":{"average":8.0}},
                {"season":1,"number":3,"airdate":"","rating":{"average":6.9}}
            ]"""
        )
        assertEquals(listOf(1, 3), episodes?.map { it.number })
        assertEquals(7.2f, episodes?.first()?.rating ?: 0f, 0.001f)
        assertEquals("2017-10-03", episodes?.first()?.aired)
        assertNull(episodes?.last()?.aired)
    }

    // An answer that is not a list is left out
    @Test
    fun anAnswerThatIsNotAListIsLeftOut() {
        assertNull(TvMazeClient.parseEpisodes("""{"error":"x"}"""))
    }
}
