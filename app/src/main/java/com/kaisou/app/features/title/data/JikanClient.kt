package com.kaisou.app.features.title.data

// Java networking
import java.io.IOException
// Project resources
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.HttpText

// Asks Jikan, the open door to MyAnimeList, trying the public mirrors in turn, the one that answered being asked first next
object JikanClient {
    private val Addresses = listOf(
        "https://api.jikan.moe/v4",
        "https://jikanfortheweebs.midnightignite.me/v4",
        "https://api.tenrai.org/v1"
    )

    // How long each address has to answer
    private const val TryMillis = 4_000

    @Volatile
    private var first = 0

    // The text of the answer for a path, null when unknown, IOException when no address answered, blocking the thread
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
