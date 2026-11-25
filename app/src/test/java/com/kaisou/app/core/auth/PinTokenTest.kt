package com.kaisou.app.core.auth

import java.util.Base64
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests of the token AniList shows for copying
class PinTokenTest {
    // Builds a token with a given body
    private fun token(body: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("{\"alg\":\"RS256\"}", body, "signature").joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }

    // A token made for this client is ours
    @Test
    fun aTokenMadeForThisClientIsOurs() {
        assertTrue(PinToken.isFor(token("{\"aud\":\"58000\",\"sub\":\"1\"}"), "58000"))
    }

    // A token made for another client is not
    @Test
    fun aTokenMadeForAnotherClientIsNot() {
        assertFalse(PinToken.isFor(token("{\"aud\":\"11111\"}"), "58000"))
    }

    // Spaces around the token do not matter
    @Test
    fun spacesAroundTheTokenDoNotMatter() {
        assertTrue(PinToken.isFor("  " + token("{\"aud\":\"58000\"}") + "\n", "58000"))
    }

    // Text that is not a token is not
    @Test
    fun textThatIsNotATokenIsNot() {
        assertFalse(PinToken.isFor("hello world", "58000"))
        assertFalse(PinToken.isFor("a.b.c", "58000"))
        assertFalse(PinToken.isFor("", "58000"))
    }

    // With no client nothing is ours
    @Test
    fun withNoClientNothingIsOurs() {
        assertFalse(PinToken.isFor(token("{\"aud\":\"\"}"), ""))
    }
}
