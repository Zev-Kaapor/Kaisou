package com.kaisou.app.core.artwork

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoCheckTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val clear = 0x00000000

    @Test
    fun aLogoDrawnInBlackIsTurnedDown() {
        assertFalse(LogoCheck.isVisibleOnDark(IntArray(100) { if (it % 2 == 0) black else clear }))
    }

    @Test
    fun aWhiteLogoShows() {
        assertTrue(LogoCheck.isVisibleOnDark(IntArray(100) { if (it % 2 == 0) white else clear }))
    }

    @Test
    fun aColoredLogoWithBlackOutlineShows() {
        assertTrue(LogoCheck.isVisibleOnDark(IntArray(100) { if (it < 20) black else white }))
    }

    @Test
    fun anEmptyPictureDoesNotShow() {
        assertFalse(LogoCheck.isVisibleOnDark(IntArray(100) { clear }))
    }
}
