package com.kaisou.app.core.artwork

// JSON
import org.json.JSONObject
// Project resources
import com.kaisou.app.BuildConfig
// Java networking and utilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// What TheTVDB has of a title, every list holds full image addresses and is empty when there is none
class TvdbArt(
    val logos: List<String>,
    val posters: List<String>,
    val title: String?
)

// A failed answer is told apart from a title that is not there, the first one is tried again later
class TvdbResult(val art: TvdbArt?, val failed: Boolean)

// Second source of artwork, for the titles TMDB has no logo, poster or translated name for.
// Uses the v4 API of TheTVDB, the key comes from local.properties
object TvdbClient {
    private const val ApiUrl = "https://api4.thetvdb.com/v4"
    private const val TimeoutMillis = 15_000
    // The token lasts a month, it is asked again well before that
    private const val TokenMillis = 20L * 60L * 60L * 1000L
    // How many images of a group are kept to choose from, and the narrowest one that is still sharp
    private const val MaxPerTitle = 10
    private const val MinPosterWidth = 1000
    private const val MinLogoWidth = 600

    // Ids of the artwork types of TheTVDB, series and movies have their own
    private const val SeriesLogo = 23
    private const val SeriesIcon = 5
    private const val SeriesPoster = 2
    private const val MovieLogo = 25
    private const val MovieIcon = 18
    private const val MoviePoster = 14

    private const val SeriesKind = "series"
    private const val MovieKind = "movies"
    private const val EnglishLanguage = "eng"

    private class Record(val kind: String, val id: Int)

    private class Artwork(val type: Int, val image: String, val score: Double, val width: Int, val language: String?)

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenExpiresAt = 0L

    // Blocks on the network, so it is called from a background thread. The language names are the three letters ones
    fun art(tvdbId: Int?, imdbId: String?, locale: Locale, needTitle: Boolean): TvdbResult {
        if (BuildConfig.TVDB_KEY.isEmpty()) return TvdbResult(null, failed = false)
        return try {
            lookup(tvdbId, imdbId, locale, needTitle)
        } catch (_: Exception) {
            TvdbResult(null, failed = true)
        }
    }

    private fun lookup(tvdbId: Int?, imdbId: String?, locale: Locale, needTitle: Boolean): TvdbResult {
        val record = resolve(tvdbId, imdbId) ?: return TvdbResult(null, failed = false)
        val language = locale.isO3Language
        val artworks = artworksOf(record)
        val movie = record.kind == MovieKind

        val logoType = if (movie) MovieLogo else SeriesLogo
        val iconType = if (movie) MovieIcon else SeriesIcon
        val posterType = if (movie) MoviePoster else SeriesPoster
        // A clear logo is what is wanted, the square icon is the next best and is cleaned when it is drawn
        val logos = pick(artworks.filter { it.type == logoType }, MinLogoWidth, { it.language == language }, { it.language == EnglishLanguage }, { it.language == null }, { true })
            .ifEmpty { pick(artworks.filter { it.type == iconType }, MinLogoWidth, { it.language == language }, { it.language == EnglishLanguage }, { it.language == null }, { true }) }
        val posters = pick(artworks.filter { it.type == posterType }, MinPosterWidth, { it.language == null }, { it.language == language }, { it.language == EnglishLanguage }, { true })

        val title = if (needTitle) translatedName(record, language) else null
        return TvdbResult(TvdbArt(logos, posters, title), failed = false)
    }

    // The record on TheTVDB: series come with their id, the others are found through the IMDb id
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

    private fun artworksOf(record: Record): List<Artwork> {
        val path = if (record.kind == MovieKind) "/movies/${record.id}/extended" else "/series/${record.id}/artworks"
        val list = call(path)?.optJSONObject("data")?.optJSONArray("artworks") ?: return emptyList()
        return (0 until list.length()).map { list.getJSONObject(it) }.mapNotNull { json ->
            val image = if (json.isNull("image")) null else json.getString("image")
            image?.let {
                Artwork(
                    type = json.optInt("type"),
                    image = it,
                    score = json.optDouble("score", 0.0),
                    width = json.optInt("width"),
                    language = if (json.isNull("language")) null else json.getString("language")
                )
            }
        }
    }

    private fun translatedName(record: Record, language: String): String? {
        val path = "/${record.kind}/${record.id}/translations/$language"
        val data = call(path)?.optJSONObject("data") ?: return null
        return if (data.isNull("name")) null else data.getString("name").takeIf { it.isNotBlank() }
    }

    // From the first group that has any: the images sharp enough to fill the screen, or the biggest one when none is,
    // and of those the best rated ones
    private fun pick(artworks: List<Artwork>, minWidth: Int, vararg groups: (Artwork) -> Boolean): List<String> {
        for (group in groups) {
            val found = artworks.filter(group)
            if (found.isEmpty()) continue
            val sharp = found.filter { it.width >= minWidth }.ifEmpty { listOf(found.maxBy { it.width }) }
            return sharp.sortedByDescending { it.score }.take(MaxPerTitle).map { it.image }
        }
        return emptyList()
    }

    // Null when TheTVDB does not know what was asked
    private fun call(path: String): JSONObject? {
        val connection = URL(ApiUrl + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${session()}")
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).readText())
                HttpURLConnection.HTTP_NOT_FOUND -> null
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
