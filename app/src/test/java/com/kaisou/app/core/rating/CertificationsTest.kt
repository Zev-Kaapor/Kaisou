package com.kaisou.app.core.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CertificationsTest {
    @Test
    fun theNumberInTheLabelIsTheAge() {
        assertEquals(14, Certifications.ageOf("TV-14"))
        assertEquals(12, Certifications.ageOf("12"))
        assertEquals(15, Certifications.ageOf("MA15+"))
        assertEquals(19, Certifications.ageOf("19"))
    }

    @Test
    fun theWordsThatStandForAnAgeAreKnown() {
        assertEquals(0, Certifications.ageOf("L"))
        assertEquals(0, Certifications.ageOf("TV-G"))
        assertEquals(17, Certifications.ageOf("TV-MA"))
        assertEquals(18, Certifications.ageOf("NC-17"))
    }

    @Test
    fun aLabelThatIsNotKnownIsNothing() {
        assertNull(Certifications.ageOf("Unrated"))
        assertNull(Certifications.ageOf(""))
    }

    @Test
    fun anAgeFallsInTheLastGroupThatStartsUnderIt() {
        assertEquals(AgeGroup.All, AgeGroup.of(7))
        assertEquals(AgeGroup.Twelve, AgeGroup.of(13))
        assertEquals(AgeGroup.Sixteen, AgeGroup.of(17))
        assertEquals(AgeGroup.Eighteen, AgeGroup.of(21))
    }
}
