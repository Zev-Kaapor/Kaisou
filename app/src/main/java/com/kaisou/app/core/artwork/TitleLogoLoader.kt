package com.kaisou.app.core.artwork

// Android graphics
import android.content.Context
import android.graphics.Bitmap
// Compose graphics
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
// Image loading
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Java utilities
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

// Loads a title logo and cleans it: a solid background is erased and the empty margin is cut,
// so every logo can be sized by what is really drawn. A logo drawn only in black is turned down, it would vanish on the page
class TitleLogoLoader {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    // Logos that were turned down, so they are not downloaded and looked at again
    private val turnedDown = ConcurrentHashMap.newKeySet<String>()

    // Null when the logo cannot be loaded or would not show on the dark page, failures to load are not cached
    suspend fun load(context: Context, url: String): ImageBitmap? {
        cache[url]?.let { return it }
        if (url in turnedDown) return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(Size(MaxSide, MaxSide))
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return null
        val cleaned = withContext(Dispatchers.Default) { clean(result.image.toBitmap()) }
        if (cleaned == null) {
            turnedDown += url
            return null
        }
        return cleaned.asImageBitmap().also { cache[url] = it }
    }

    // Null for a logo drawn only in black. The one over a white background that fills most of the image is the exception,
    // it keeps the background and shows as a white plate
    private fun clean(source: Bitmap): Bitmap? {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val solid = hasSolidBackground(pixels, width, height)
        val background = pixels[0]
        if (isDarkDrawing(pixels, if (solid) background else null)) {
            return if (solid && isWhitePlate(pixels, background)) source else null
        }
        if (solid) clearBackground(pixels, width, height)

        // Smallest rectangle that holds every visible pixel
        var top = height
        var bottom = -1
        var left = width
        var right = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (alphaOf(pixels[y * width + x]) > AlphaThreshold) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    bottom = y
                }
            }
        }
        // Nothing visible, keep the image as it came
        if (bottom < 0) return source
        return Bitmap.createBitmap(
            pixels,
            top * width + left,
            width,
            right - left + 1,
            bottom - top + 1,
            Bitmap.Config.ARGB_8888
        )
    }

    // The four corners are opaque and of the same color
    private fun hasSolidBackground(pixels: IntArray, width: Int, height: Int): Boolean {
        val corners = intArrayOf(
            pixels[0],
            pixels[width - 1],
            pixels[(height - 1) * width],
            pixels[height * width - 1]
        )
        return corners.all { alphaOf(it) == OPAQUE && distance(it, corners[0]) <= BackgroundTolerance }
    }

    // Erases the background color from the borders inward, what is enclosed by the drawing stays
    private fun clearBackground(pixels: IntArray, width: Int, height: Int) {
        val background = pixels[0]
        val stack = IntArray(width * height)
        var size = 0
        fun visit(index: Int) {
            val pixel = pixels[index]
            if (alphaOf(pixel) != 0 && distance(pixel, background) <= BackgroundTolerance) {
                pixels[index] = 0
                stack[size++] = index
            }
        }
        for (x in 0 until width) {
            visit(x)
            visit((height - 1) * width + x)
        }
        for (y in 0 until height) {
            visit(y * width)
            visit(y * width + width - 1)
        }
        while (size > 0) {
            val index = stack[--size]
            val x = index % width
            val y = index / width
            if (x > 0) visit(index - 1)
            if (x < width - 1) visit(index + 1)
            if (y > 0) visit(index - width)
            if (y < height - 1) visit(index + width)
        }
    }

    // Almost everything drawn is black, the pixels of the background when there is one are not counted
    private fun isDarkDrawing(pixels: IntArray, background: Int?): Boolean {
        var drawn = 0
        var dark = 0
        for (pixel in pixels) {
            if (alphaOf(pixel) <= AlphaThreshold) continue
            if (background != null && distance(pixel, background) <= BackgroundTolerance) continue
            drawn++
            if (brightest(pixel) <= DarkChannel) dark++
        }
        return drawn > 0 && dark >= drawn * DarkShare
    }

    // The background is white and covers most of the image
    private fun isWhitePlate(pixels: IntArray, background: Int): Boolean {
        if (darkest(background) < WhiteChannel) return false
        val covered = pixels.count { distance(it, background) <= BackgroundTolerance }
        return covered >= pixels.size * WhitePlateShare
    }

    private fun alphaOf(pixel: Int) = pixel ushr ALPHA_SHIFT

    private fun brightest(pixel: Int) =
        maxOf((pixel shr RED_SHIFT) and CHANNEL_MASK, (pixel shr GREEN_SHIFT) and CHANNEL_MASK, pixel and CHANNEL_MASK)

    private fun darkest(pixel: Int) =
        minOf((pixel shr RED_SHIFT) and CHANNEL_MASK, (pixel shr GREEN_SHIFT) and CHANNEL_MASK, pixel and CHANNEL_MASK)

    // Largest difference between the color channels of two pixels
    private fun distance(a: Int, b: Int): Int {
        val red = abs(((a shr RED_SHIFT) and CHANNEL_MASK) - ((b shr RED_SHIFT) and CHANNEL_MASK))
        val green = abs(((a shr GREEN_SHIFT) and CHANNEL_MASK) - ((b shr GREEN_SHIFT) and CHANNEL_MASK))
        val blue = abs((a and CHANNEL_MASK) - (b and CHANNEL_MASK))
        return maxOf(red, green, blue)
    }

    companion object {
        val Shared = TitleLogoLoader()

        // Longest side the logo is decoded at, above what the screen can show of a logo
        private const val MaxSide = 2048
        // Pixels with an alpha at or under this count as empty
        private const val AlphaThreshold = 16
        // How far a color may be from the background and still be background
        private const val BackgroundTolerance = 40
        // A pixel is black when no channel goes over this, and a logo is black when this share of what is drawn is
        private const val DarkChannel = 64
        private const val DarkShare = 0.9f
        // A background is white when no channel is under this, and it makes a plate when it covers this share of the image
        private const val WhiteChannel = 230
        private const val WhitePlateShare = 0.8f
        private const val OPAQUE = 255
        private const val ALPHA_SHIFT = 24
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val CHANNEL_MASK = 0xFF
    }
}
