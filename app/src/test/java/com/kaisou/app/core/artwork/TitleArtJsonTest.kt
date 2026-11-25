package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the keeping of the art of a title as text
class TitleArtJsonTest {
    // The art comes back as it was kept
    @Test
    fun theArtComesBackAsItWasKept() {
        val art = TitleArt(
            posterUrl = "https://image.tmdb.org/t/p/original/a.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/original/b.jpg",
            logos = listOf("https://x/1.png", "https://x/2.png"),
            certifications = mapOf("BR" to "14", "US" to "TV-14"),
            tmdbKind = "tv",
            tmdbId = 37854
        )
        val back = TitleArtJson.read(TitleArtJson.write(art))!!
        assertEquals(art.posterUrl, back.posterUrl)
        assertEquals(art.backdropUrl, back.backdropUrl)
        assertEquals(art.logos, back.logos)
        assertEquals(art.certifications, back.certifications)
        assertEquals(art.tmdbKind, back.tmdbKind)
        assertEquals(art.tmdbId, back.tmdbId)
    }

    // A title with nothing is kept too
    @Test
    fun aTitleWithNothingIsKeptToo() {
        val back = TitleArtJson.read(TitleArtJson.write(TitleArt.None))!!
        assertNull(back.posterUrl)
        assertEquals(emptyList<String>(), back.logos)
    }

    // A text that is not one of these is nothing
    @Test
    fun aTextThatIsNotOneOfTheseIsNothing() {
        assertNull(TitleArtJson.read("not json"))
        assertNull(TitleArtJson.read("{\"poster\":1}"))
    }
}
