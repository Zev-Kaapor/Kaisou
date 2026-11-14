package com.kaisou.app.core.auth

import java.util.Base64
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTokenTest {
    private fun token(body: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("{\"alg\":\"RS256\"}", body, "signature").joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }

    @Test
    fun aTokenMadeForThisClientIsOurs() {
        assertTrue(PinToken.isFor(token("{\"aud\":\"58000\",\"sub\":\"1\"}"), "58000"))
    }

    @Test
    fun aTokenMadeForAnotherClientIsNot() {
        assertFalse(PinToken.isFor(token("{\"aud\":\"11111\"}"), "58000"))
    }

    @Test
    fun spacesAroundTheTokenDoNotMatter() {
        assertTrue(PinToken.isFor("  " + token("{\"aud\":\"58000\"}") + "\n", "58000"))
    }

    @Test
    fun textThatIsNotATokenIsNot() {
        assertFalse(PinToken.isFor("hello world", "58000"))
        assertFalse(PinToken.isFor("a.b.c", "58000"))
        assertFalse(PinToken.isFor("", "58000"))
    }

    @Test
    fun withNoClientNothingIsOurs() {
        assertFalse(PinToken.isFor(token("{\"aud\":\"\"}"), ""))
    }
}
