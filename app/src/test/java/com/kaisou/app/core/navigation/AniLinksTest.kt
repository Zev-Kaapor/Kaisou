package com.kaisou.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AniLinksTest {
    @Test
    fun theAddressesOfATitleOrAPersonOpenThePagesOfTheApp() {
        assertEquals("title/154587", aniLinkRoute("https://anilist.co/anime/154587/Sousou-no-Frieren/"))
        assertEquals("title/30013", aniLinkRoute("https://anilist.co/manga/30013"))
        assertEquals("profile/character/1", aniLinkRoute("http://www.anilist.co/character/1/Spike-Spiegel"))
        assertEquals("profile/staff/119353", aniLinkRoute("https://anilist.co/staff/119353"))
        assertEquals("profile/studio/6", aniLinkRoute("https://anilist.co/studio/6/GAINAX"))
    }

    @Test
    fun anyOtherAddressGoesToTheBrowser() {
        assertNull(aniLinkRoute("https://anilist.co/user/ZevKaapor"))
        assertNull(aniLinkRoute("https://example.org/anime/12"))
        assertNull(aniLinkRoute("https://anilist.co/anime/abc"))
    }
}
