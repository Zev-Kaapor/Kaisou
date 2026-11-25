package com.kaisou.app.core.net

// Android logging
import android.util.Log
// Java networking and concurrency
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.Semaphore

// What a service answers when it has been asked too much: the wait it asks for, in milliseconds
class TooManyRequests(val retryAfterMillis: Long) : IOException("Too many requests") {
    // Holds the code of the answer that asks for a rest
    companion object {
        // The code of the answer, which HttpURLConnection has no name for
        const val Code = 429
        private const val DefaultWaitMillis = 4_000L
        private const val MillisPerSecond = 1_000L

        // The wait the answer asks for in its Retry-After header, a few seconds when it does not say
        fun of(connection: HttpURLConnection): TooManyRequests {
            val seconds = connection.getHeaderField("Retry-After")?.trim()?.toLongOrNull()
            return TooManyRequests(seconds?.times(MillisPerSecond) ?: DefaultWaitMillis)
        }
    }
}

// The gate of one service: a few requests at once, and when it says too many all of them wait, then the turned away ones go again
class RateGate(
    private val name: String,
    maxAtOnce: Int,
    private val log: (String) -> Unit = { Log.w(Tag, it) }
) {
    private val permits = Semaphore(maxAtOnce, true)

    @Volatile
    private var openAtMillis = 0L

    // Told how long the gate will rest, in milliseconds, whenever a service closes it, so the screen can tell why things are slow
    @Volatile
    var onRest: ((Long) -> Unit)? = null

    // Rests the gate until the moment a service says its limit comes back, so requests wait instead of being turned away
    fun restUntil(reopenMillis: Long) {
        val capped = reopenMillis.coerceAtMost(System.currentTimeMillis() + MaxEarlyWaitMillis)
        if (capped > openAtMillis) openAtMillis = capped
    }

    // Makes the request when the gate is open, and again when it was turned away, a few times, other failures going up as they are
    fun <T> run(request: () -> T): T {
        var turnedAway = 0
        while (true) {
            permits.acquire()
            try {
                val wait = openAtMillis - System.currentTimeMillis()
                if (wait > 0) Thread.sleep(wait)
                return request()
            } catch (error: TooManyRequests) {
                val wait = error.retryAfterMillis.coerceAtMost(MaxWaitMillis)
                val reopen = System.currentTimeMillis() + wait
                if (reopen > openAtMillis) {
                    openAtMillis = reopen
                    onRest?.invoke(wait)
                }
                log("$name asked for ${wait}ms of rest")
                if (++turnedAway > MaxRetries) throw error
            } finally {
                permits.release()
            }
        }
    }

    // Constants of the gate
    private companion object {
        // Name of the log tag of the gate
        const val Tag = "RateGate"

        // The longest a request waits for a service to rest, and how many times it goes again
        const val MaxWaitMillis = 20_000L

        // The longest the gate rests before it is turned away, for a limit that the service says is nearly used
        const val MaxEarlyWaitMillis = 60_000L
        const val MaxRetries = 2
    }
}

// The gate of each service the app asks. The numbers are how many requests go at once
object Gates {
    val AniList = RateGate("AniList", maxAtOnce = 2)
    val AniZip = RateGate("ani.zip", maxAtOnce = 3)
    val Tmdb = RateGate("TMDB", maxAtOnce = 6)
    val Tvdb = RateGate("TheTVDB", maxAtOnce = 4)
    val TvMaze = RateGate("TVmaze", maxAtOnce = 2)
    val Imdb = RateGate("IMDb", maxAtOnce = 2)
    val Mal = RateGate("MyAnimeList", maxAtOnce = 3)
    val Google = RateGate("Google Translate", maxAtOnce = 3)
    val Jikan = RateGate("Jikan", maxAtOnce = 1)
    val News = RateGate("News", maxAtOnce = 2)
    val MangaDex = RateGate("MangaDex", maxAtOnce = 2)
}
