package com.kaisou.app.core.artwork

// JSON parsing
import org.json.JSONObject
// Project resources
import com.kaisou.app.BuildConfig
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests
// Java networking and utilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale

// Second source of posters, logos and backdrops, TheTVDB, which has some that TMDB does not. Uses the v4 API, the key comes from local.properties
object TvdbClient {
    private const val ApiUrl = "https://api4.thetvdb.com/v4"
    private const val TimeoutMillis = 8_000
    // The token lasts a month, it is asked again well before that
    private const val TokenMillis = 20L * 60L * 60L * 1000L

    // Ids of the artwork types on TheTVDB, series and movies have their own
    private const val SeriesPoster = 2
    private const val MoviePoster = 14
    private const val SeriesLogo = 23
    private const val MovieLogo = 25
    private const val SeriesBackground = 3
    private const val MovieBackground = 15

    private const val SeriesKind = "series"
    // How many pages of episodes are read at most, and how long a date written as 2026-10-03 is
    private const val MaxEpisodePages = 6
    private const val DateLength = 10
    private const val SeasonCacheMillis = 6L * 60L * 60L * 1000L
    private const val SeasonCacheEntries = 100
    private const val MovieKind = "movies"

    private class Record(val kind: String, val id: Int)

    // The days the seasons first aired by series, so a series is not read again for each of its seasons
    private val seasonCache = TimedCache<Map<Int, LocalDate>>(SeasonCacheMillis, SeasonCacheEntries)

    // The languages of TheTVDB have three letters, the app works with two
    private val twoLetters: Map<String, String> by lazy {
        Locale.getISOLanguages().associateBy { Locale.forLanguageTag(it).isO3Language }
    }

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenExpiresAt = 0L

    val isSet: Boolean get() = BuildConfig.TVDB_KEY.isNotEmpty()

    // Logs in ahead of time, so the first poster does not wait for it. Blocks on the network
    fun warmUp() {
        if (isSet) runCatching { session() }
    }

    // The posters of a title: a series comes with its id, the others are found through the IMDb id. Blocks on the network
    fun posters(tvdbId: Int?, imdbId: String?): Artworks = artworks(tvdbId, imdbId, SeriesPoster, MoviePoster)

    // The logos of a title, found the same way. Blocks on the network
    fun logos(tvdbId: Int?, imdbId: String?): Artworks = artworks(tvdbId, imdbId, SeriesLogo, MovieLogo)

    // The backdrops of a title, the wide pictures, found the same way. Blocks on the network
    fun backdrops(tvdbId: Int?, imdbId: String?): Artworks = artworks(tvdbId, imdbId, SeriesBackground, MovieBackground)

    private fun artworks(tvdbId: Int?, imdbId: String?, seriesType: Int, movieType: Int): Artworks {
        if (!isSet) return Artworks.None
        return try {
            val record = resolve(tvdbId, imdbId) ?: return Artworks.None
            val movie = record.kind == MovieKind
            val path = if (movie) "/movies/${record.id}/extended" else "/series/${record.id}/artworks?type=$seriesType"
            val type = if (movie) movieType else seriesType
            val list = call(path)?.optJSONObject("data")?.optJSONArray("artworks") ?: return Artworks.None
            Artworks(
                list = (0 until list.length()).map { list.getJSONObject(it) }.mapNotNull { json ->
                    val image = if (json.isNull("image")) null else json.getString("image")
                    if (image == null || json.optInt("type") != type) {
                        null
                    } else {
                        Artwork(
                            url = image,
                            width = json.optInt("width"),
                            height = json.optInt("height"),
                            language = if (json.isNull("language")) null else json.getString("language").let { code -> twoLetters[code] ?: code },
                            score = json.optDouble("score", 0.0)
                        )
                    }
                },
                failed = false
            )
        } catch (_: Exception) {
            Artworks.Failed
        }
    }

    // The day each season of a series first aired, from its episodes in the official order. Specials, which are season 0, are left
    // out. The episodes come in pages, a long series has a few. Blocks on the network
    fun seasonStarts(tvdbId: Int): SeasonStarts {
        if (!isSet) return SeasonStarts.None
        seasonCache.get(tvdbId)?.let { return SeasonStarts(it.value, failed = false) }
        return try {
            val starts = HashMap<Int, LocalDate>()
            for (page in 0 until MaxEpisodePages) {
                val json = call("/series/$tvdbId/episodes/default?page=$page") ?: break
                val episodes = json.optJSONObject("data")?.optJSONArray("episodes") ?: break
                for (index in 0 until episodes.length()) {
                    val episode = episodes.getJSONObject(index)
                    val season = episode.optInt("seasonNumber")
                    val aired = if (episode.isNull("aired")) null else runCatching { LocalDate.parse(episode.getString("aired").take(DateLength)) }.getOrNull()
                    if (season > 0 && aired != null) starts.merge(season, aired) { a, b -> if (a.isBefore(b)) a else b }
                }
                if (json.optJSONObject("links")?.isNull("next") != false) break
            }
            seasonCache.put(tvdbId, starts)
            SeasonStarts(starts, failed = false)
        } catch (_: Exception) {
            SeasonStarts.Failed
        }
    }

    private fun resolve(tvdbId: Int?, imdbId: String?): Record? {
        if (tvdbId != null) return Record(SeriesKind, tvdbId)
        if (imdbId == null) return null
        val data = call("/search/remoteid/$imdbId")?.optJSONArray("data") ?: return null
        for (index in 0 until data.length()) {
            val entry = data.getJSONObject(index)
            entry.optJSONObject("series")?.let { return Record(SeriesKind, it.getInt("id")) }
            entry.optJSONObject("movie")?.let { return Record(MovieKind, it.getInt("id")) }
        }
        return null
    }

    // Null when TheTVDB does not know what was asked
    private fun call(path: String): JSONObject? = Gates.Tvdb.run { callOnce(path) }

    private fun callOnce(path: String): JSONObject? {
        val connection = URL(ApiUrl + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${session()}")
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).readText())
                HttpURLConnection.HTTP_NOT_FOUND -> null
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    // The token was refused, a new one is asked next time
                    token = null
                    throw IOException("TheTVDB refused the token")
                }
                else -> throw IOException("TheTVDB answered ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    // The token of the session, logged in again when it is old
    private fun session(): String {
        val current = token
        if (current != null && System.currentTimeMillis() < tokenExpiresAt) return current
        val connection = URL("$ApiUrl/login").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(JSONObject().put("apikey", BuildConfig.TVDB_KEY).toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode == TooManyRequests.Code) throw TooManyRequests.of(connection)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("TheTVDB login answered ${connection.responseCode}")
            val fresh = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).readText()).getJSONObject("data").getString("token")
            token = fresh
            tokenExpiresAt = System.currentTimeMillis() + TokenMillis
            return fresh
        } finally {
            connection.disconnect()
        }
    }
}
