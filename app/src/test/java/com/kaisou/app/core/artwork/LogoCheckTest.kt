package com.kaisou.app.core.artwork

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests of the check that a logo can be seen on the dark
class LogoCheckTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val clear = 0x00000000

    // A logo drawn in black is turned down
    @Test
    fun aLogoDrawnInBlackIsTurnedDown() {
        assertFalse(LogoCheck.isVisibleOnDark(IntArray(100) { if (it % 2 == 0) black else clear }))
    }

    // A white logo shows
    @Test
    fun aWhiteLogoShows() {
        assertTrue(LogoCheck.isVisibleOnDark(IntArray(100) { if (it % 2 == 0) white else clear }))
    }

    // A colored logo with a black outline shows
    @Test
    fun aColoredLogoWithBlackOutlineShows() {
        assertTrue(LogoCheck.isVisibleOnDark(IntArray(100) { if (it < 20) black else white }))
    }

    // An empty picture does not show
    @Test
    fun anEmptyPictureDoesNotShow() {
        assertFalse(LogoCheck.isVisibleOnDark(IntArray(100) { clear }))
    }
}
