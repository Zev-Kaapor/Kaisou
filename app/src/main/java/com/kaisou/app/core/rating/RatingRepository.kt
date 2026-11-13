package com.kaisou.app.core.rating

// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
// JSON parsing
import org.json.JSONObject
// Project resources
import com.kaisou.app.BuildConfig
import com.kaisou.app.core.artwork.TitleArtRepository
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.model.ContentRating
import com.kaisou.app.core.model.MalRating
// Java networking
import java.net.HttpURLConnection
import java.net.URL
// Java concurrency
import java.util.concurrent.ConcurrentHashMap

// Finds the advisory rating of a title: the official Brazilian one when the artwork already brought it, or the one of
// MyAnimeList, one small request by the id AniList gives, turned into the Brazilian symbols when the app is in Portuguese
class RatingRepository {
    private class Fetched(val value: MalRating?, val failed: Boolean)

    // Ratings of MyAnimeList by its id, an empty one means the title has none there
    private val mal = TimedCache<MalRating?>(CacheMillis, MaxEntries)

    // The requests going on by id, so the card and the shelf that asked ahead share one, and it goes on when the card
    // leaves the screen. A few go at a time, in the order they were asked
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap<Int, Deferred<MalRating?>>()
    private val requests = Semaphore(MaxRequests)

    // The rating already known, without asking anything, so a card that comes back to the screen shows it at once
    fun known(anilistId: Int, malId: Int?, brazilian: Boolean): ContentRating? {
        if (brazilian) official(anilistId)?.let { return it }
        return malId?.let { mal.get(it)?.value }?.let { shown(it, brazilian) }
    }

    suspend fun load(anilistId: Int, malId: Int?, brazilian: Boolean): ContentRating? {
        if (brazilian) official(anilistId)?.let { return it }
        if (malId == null || BuildConfig.MAL_CLIENT_ID.isEmpty()) return null
        val found = mal.get(malId)?.value ?: request(malId).await()
        return found?.let { shown(it, brazilian) }
    }

    // Asks for the ratings of titles before their cards show, the ones already known or on the way are left alone
    fun prefetch(malIds: List<Int>) {
        if (BuildConfig.MAL_CLIENT_ID.isEmpty()) return
        malIds.distinct().filter { mal.get(it) == null }.forEach(::request)
    }

    private fun request(malId: Int): Deferred<MalRating?> {
        val request = pending.computeIfAbsent(malId) {
            scope.async(start = CoroutineStart.LAZY) {
                requests.withPermit {
                    val fetched = fetch(malId)
                    // A failure is not kept, so it is asked again next time
                    if (fetched.failed.not()) mal.put(malId, fetched.value)
                    fetched.value
                }
            }.also { job -> job.invokeOnCompletion { pending.remove(malId) } }
        }
        request.start()
        return request
    }

    private fun shown(rating: MalRating, brazilian: Boolean): ContentRating =
        if (brazilian) ContentRating.Brazilian(rating.brazilian) else ContentRating.Mal(rating)

    private fun official(anilistId: Int): ContentRating? =
        TitleArtRepository.Shared.knownRating(anilistId)?.let(ContentRating::Brazilian)

    // A title MyAnimeList does not know has no rating, any other failure is asked again next time
    private fun fetch(malId: Int): Fetched {
        val connection = URL("$MalUrl/anime/$malId?fields=rating").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    Fetched(MalRating.fromCode(JSONObject(body).optString("rating")), failed = false)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> Fetched(null, failed = false)
                else -> Fetched(null, failed = true)
            }
        } catch (_: Exception) {
            Fetched(null, failed = true)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        // One instance for the whole app, so the ratings found stay in memory
        val Shared = RatingRepository()

        private const val MalUrl = "https://api.myanimelist.net/v2"
        private const val TimeoutMillis = 15_000
        private const val MaxRequests = 6
        // How long a rating is kept and how many titles at most
        private const val CacheMillis = 60L * 60L * 1000L
        private const val MaxEntries = 500
    }
}
