package com.kaisou.app.core.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the rating of MyAnimeList
class MalRatingTest {
    // The codes of the API are read
    @Test
    fun theCodesOfTheApiAreRead() {
        assertEquals(MalRating.Teens, MalRating.fromCode("pg_13"))
        assertEquals(MalRating.MildNudity, MalRating.fromCode("r+"))
        assertNull(MalRating.fromCode(""))
    }

    // They fall in the groups that were measured
    @Test
    fun theyFallInTheGroupsThatWereMeasured() {
        assertEquals(AgeGroup.All, MalRating.General.group)
        assertEquals(AgeGroup.Ten, MalRating.Children.group)
        assertEquals(AgeGroup.Fourteen, MalRating.Teens.group)
        assertEquals(AgeGroup.Sixteen, MalRating.Violence.group)
        assertEquals(AgeGroup.Sixteen, MalRating.MildNudity.group)
    }
}
