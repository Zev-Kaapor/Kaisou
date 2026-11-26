package com.kaisou.app.core.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the gallery made from the pictures of both sites
class GalleryBuilderTest {
    private val tmdb = "https://image.tmdb.org/t/p/original"

    // A picture with the language and the rating it may have
    private fun art(url: String, language: String? = null, score: Double = 0.0, thumbnail: String? = null, width: Int = 2000) = Artwork(url, width, width * 3 / 2, language, score, thumbnail)

    // A picture of TMDB gets a small version by the size in its address, and one of TheTVDB keeps the small one it came with
    @Test
    fun theSmallPictureComesFromTheSizeOrFromTheSite() {
        val gallery = GalleryBuilder.of(listOf(art("$tmdb/a.jpg"), art("https://artworks.thetvdb.com/b.jpg", thumbnail = "https://artworks.thetvdb.com/b_t.jpg")), emptyList(), emptyList())
        assertEquals("https://image.tmdb.org/t/p/w342/a.jpg", gallery.posters[0].thumbnail)
        assertEquals("https://artworks.thetvdb.com/b_t.jpg", gallery.posters[1].thumbnail)
    }

    // The same address is not listed twice and the best rated comes first
    @Test
    fun noPictureTwiceAndTheBestRatedFirst() {
        val gallery = GalleryBuilder.of(listOf(art("$tmdb/a.jpg", score = 1.0), art("$tmdb/b.jpg", score = 5.0), art("$tmdb/a.jpg", score = 1.0)), emptyList(), emptyList())
        assertEquals(listOf("$tmdb/b.jpg", "$tmdb/a.jpg"), gallery.posters.map { it.url })
    }

    // The pictures in the language of the user come first, then the ones with no text, then English, keeping the order inside each
    @Test
    fun theLanguageOfTheUserComesFirst() {
        val gallery = GalleryBuilder.of(
            listOf(art("$tmdb/1.jpg", "ja", 9.0), art("$tmdb/2.jpg", "en", 8.0), art("$tmdb/3.jpg", null, 7.0), art("$tmdb/4.jpg", "pt", 6.0)),
            emptyList(),
            emptyList()
        ).inLanguage("pt")
        assertEquals(listOf("4", "3", "2", "1"), gallery.posters.map { it.url.substringAfterLast('/').substringBefore('.') })
    }

    // Only the big pictures are kept: 1000 wide for a poster or a logo, full HD for a backdrop
    @Test
    fun onlyTheBigPicturesAreKept() {
        val gallery = GalleryBuilder.of(
            posters = listOf(art("$tmdb/p1.jpg", width = 1000), art("$tmdb/p2.jpg", width = 999), art("$tmdb/p3.jpg", width = 0)),
            backdrops = listOf(art("$tmdb/b1.jpg", width = 1920), art("$tmdb/b2.jpg", width = 1280)),
            logos = listOf(art("$tmdb/l1.png", width = 1000), art("$tmdb/l2.png", width = 500))
        )
        assertEquals(listOf("$tmdb/p1.jpg"), gallery.posters.map { it.url })
        assertEquals(listOf("$tmdb/b1.jpg"), gallery.backdrops.map { it.url })
        assertEquals(listOf("$tmdb/l1.png"), gallery.logos.map { it.url })
    }

    // What is written to be kept is read back the same, and a broken text gives nothing
    @Test
    fun whatIsKeptIsReadBackTheSame() {
        val gallery = GalleryBuilder.of(listOf(art("$tmdb/a.jpg", "pt", 3.0)), listOf(art("$tmdb/b.jpg")), listOf(art("$tmdb/c.png", "en")))
        val back = GalleryBuilder.read(GalleryBuilder.write(gallery))
        assertNotNull(back)
        assertEquals("pt", back!!.posters.single().language)
        assertNull(back.backdrops.single().language)
        assertEquals(2000, back.logos.single().width)
        assertNull(GalleryBuilder.read("not json"))
    }
}
