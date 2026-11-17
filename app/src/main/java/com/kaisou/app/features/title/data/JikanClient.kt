package com.kaisou.app.features.title.data

// Java networking
import java.io.IOException
// Project resources
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.HttpText

// Asks Jikan, the open door to the data of MyAnimeList. Its own address is often out of reach, so the same question is made to the public
// mirrors that give the same answers, one after the other, and the one that answered is the first to be asked next time
object JikanClient {
    private val Addresses = listOf(
        "https://api.jikan.moe/v4",
        "https://jikanfortheweebs.midnightignite.me/v4",
        "https://api.tenrai.org/v1"
    )
    private const val TryMillis = 4_000

    @Volatile
    private var first = 0

    // The text of the answer for a path, null when MyAnimeList does not know it, and an IOException when no address could answer. The
    // request blocks the thread, so it is made from a background one
    fun get(path: String): String? {
        var failure: Exception? = null
        for (step in Addresses.indices) {
            val index = (first + step) % Addresses.size
            try {
                val text = Gates.Jikan.run { HttpText.get(Addresses[index] + path, TryMillis) }
                first = index
                return text
            } catch (error: Exception) {
                failure = error
            }
        }
        throw IOException("Jikan could not be reached", failure)
    }
}
