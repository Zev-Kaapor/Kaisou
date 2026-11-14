package com.kaisou.app.core.artwork

// Android graphics
import android.graphics.Bitmap
import androidx.core.graphics.get

// Tells if a logo shows on the dark page: the ones drawn only in black cannot be seen, so they are turned down
object LogoCheck {
    // How many points are looked at, across and down
    private const val Samples = 64
    // A point is part of the drawing when it is at least this opaque, and it is dark when its brightness, from 0 to 255, is under the level
    private const val OpaqueAlpha = 128
    private const val DarkLevel = 60
    // The logo is turned down when this share of its drawing is dark
    private const val DarkShare = 0.9f

    private const val RedWeight = 2126
    private const val GreenWeight = 7152
    private const val BlueWeight = 722
    private const val WeightTotal = 10000

    fun isVisibleOnDark(bitmap: Bitmap): Boolean =
        isVisibleOnDark(
            IntArray(Samples * Samples) { index ->
                val x = ((index % Samples) + 0.5f) / Samples * bitmap.width
                val y = ((index / Samples) + 0.5f) / Samples * bitmap.height
                bitmap[x.toInt().coerceIn(0, bitmap.width - 1), y.toInt().coerceIn(0, bitmap.height - 1)]
            }
        )

    // The pixels are colors with the alpha in the high byte. A picture with no drawing at all is not visible either
    fun isVisibleOnDark(pixels: IntArray): Boolean {
        var drawn = 0
        var dark = 0
        for (pixel in pixels) {
            if ((pixel ushr 24) < OpaqueAlpha) continue
            drawn++
            val brightness = (RedWeight * ((pixel shr 16) and 0xFF) + GreenWeight * ((pixel shr 8) and 0xFF) + BlueWeight * (pixel and 0xFF)) / WeightTotal
            if (brightness < DarkLevel) dark++
        }
        return drawn > 0 && dark.toFloat() / drawn < DarkShare
    }
}
