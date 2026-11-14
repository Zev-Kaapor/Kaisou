package com.kaisou.app.core.rating

// JSON parsing
import org.json.JSONObject
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Project resources
import com.kaisou.app.BuildConfig
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests
// Java networking
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// The rating of an anime on MyAnimeList, one small request by the id AniList gives. Mangas have none there. The Client ID comes
// from local.properties
object MalClient {
    private const val ApiUrl = "https://api.myanimelist.net/v2"
    private const val TimeoutMillis = 10_000
    private const val CacheMillis = 60L * 60L * 1000L
    private const val MaxEntries = 500

    // What is kept on the disk and for how long: a week, the rating of a title almost never changes. A title with no rating is kept
    // as the word none
    private const val StoreSpace = "mal"
    private const val StoreMillis = 7L * 24L * 60L * 60L * 1000L
    private const val None = "none"

    // The rating found by id, a null one means the title has none there, so it is not asked again
    private val cache = TimedCache<MalRating?>(CacheMillis, MaxEntries)

    val isSet: Boolean get() = BuildConfig.MAL_CLIENT_ID.isNotEmpty()

    // Null when MyAnimeList has none or the answer failed, a failure is not kept so it is asked again next time
    suspend fun rating(malId: Int): MalRating? {
        if (!isSet) return null
        cache.get(malId)?.let { return it.value }
        return withContext(Dispatchers.IO) {
            AppStore.get(StoreSpace, malId.toString(), StoreMillis)?.let { kept ->
                val rating = MalRating.fromCode(kept)
                cache.put(malId, rating)
                return@withContext rating
            }
            try {
                Gates.Mal.run { fetch(malId) }
            } catch (_: Exception) {
                null
            }
        }
    }

    // Asks for the rating and keeps it, a title MyAnimeList does not know has none
    private fun fetch(malId: Int): MalRating? {
        val connection = URL("$ApiUrl/anime/$malId?fields=rating").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    MalRating.fromCode(JSONObject(body).optString("rating")).also { keep(malId, it) }
                }
                HttpURLConnection.HTTP_NOT_FOUND -> null.also { keep(malId, null) }
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("MyAnimeList answered ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun keep(malId: Int, rating: MalRating?) {
        cache.put(malId, rating)
        AppStore.put(StoreSpace, malId.toString(), rating?.code ?: None)
    }
}
