package com.kaisou.app.features.title.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MangaArtClientTest {
    @Test
    fun theMangaIsTheOneThatHasTheIdOfAniListInItsLinks() {
        val body = """{"data":[
            {"id":"aaa","attributes":{"links":{"al":"1","mal":"2"}}},
            {"id":"bbb","attributes":{"links":{"al":"132029"}}},
            {"id":"ccc","attributes":{}}]}"""
        assertEquals("bbb", MangaArtClient.findManga(body, 132029))
        assertNull(MangaArtClient.findManga(body, 999))
    }

    @Test
    fun theCoversComeByVolumeWithTheLanguageOfTheUserFirst() {
        val body = """{"manga":"m1","covers":[
            {"attributes":{"volume":"2","locale":"en","fileName":"two-en.jpg"}},
            {"attributes":{"volume":"1","locale":"ja","fileName":"one-ja.jpg"}},
            {"attributes":{"volume":"1","locale":"pt-br","fileName":"one-pt.jpg"}},
            {"attributes":{"volume":null,"locale":"en","fileName":"extra.jpg"}}]}"""
        val covers = MangaArtClient.parse(body, listOf("pt-br", "pt", "en", "ja"))
        assertEquals(listOf("one-pt.jpg", "one-ja.jpg", "two-en.jpg", "extra.jpg"), covers.map { it.original.substringAfterLast('/') })
        assertEquals("https://uploads.mangadex.org/covers/m1/one-pt.jpg", MangaArtClient.poster(covers)?.original)
        assertEquals("https://uploads.mangadex.org/covers/m1/one-pt.jpg.512.jpg", covers[0].thumbnail)
    }

    @Test
    fun aMangaThatIsNotThereHasNoCovers() {
        assertEquals(0, MangaArtClient.parse("{\"covers\":[]}", listOf("en")).size)
    }
}
