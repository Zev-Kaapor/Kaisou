package com.kaisou.app.core.artwork

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleArtJsonTest {
    @Test
    fun theArtComesBackAsItWasKept() {
        val art = TitleArt(
            posterUrl = "https://image.tmdb.org/t/p/original/a.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/b.jpg",
            logos = listOf("https://x/1.png", "https://x/2.png"),
            certifications = mapOf("BR" to "14", "US" to "TV-14"),
            seasonStarts = mapOf(1 to LocalDate.of(2022, 9, 13), 2 to LocalDate.of(2026, 10, 20))
        )
        val back = TitleArtJson.read(TitleArtJson.write(art))!!
        assertEquals(art.posterUrl, back.posterUrl)
        assertEquals(art.backdropUrl, back.backdropUrl)
        assertEquals(art.logos, back.logos)
        assertEquals(art.certifications, back.certifications)
        assertEquals(art.seasonStarts, back.seasonStarts)
    }

    @Test
    fun aTitleWithNothingIsKeptToo() {
        val back = TitleArtJson.read(TitleArtJson.write(TitleArt.None))!!
        assertNull(back.posterUrl)
        assertEquals(emptyList<String>(), back.logos)
    }

    @Test
    fun aTextThatIsNotOneOfTheseIsNothing() {
        assertNull(TitleArtJson.read("not json"))
        assertNull(TitleArtJson.read("{\"poster\":1}"))
    }
}
