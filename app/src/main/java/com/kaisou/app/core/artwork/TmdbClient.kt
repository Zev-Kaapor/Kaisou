package com.kaisou.app.core.artwork

// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.kaisou.app.BuildConfig
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests
// Java networking and utilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

// A result of a search by name: its id, the names it goes by, the year it started and how many people rated it
class TmdbHit(val id: Int, val names: List<String>, val year: Int?, val votes: Int)

// An episode of a season on TMDB: its number, day, name, what it tells and how it was scored by how many people
class TmdbEpisode(
    val number: Int,
    val airDate: String?,
    val name: String,
    val overview: String,
    val vote: Float,
    val votes: Int,
    val still: String? = null,
    val minutes: Int? = null
)

// First source of posters and logos, the images and the search of TMDB. The token comes from local.properties
object TmdbClient {
    // Addresses of the API and of its images, the time to wait and the length of a year in a date
    private const val ApiUrl = "https://api.themoviedb.org/3"
    private const val ImageUrl = "https://image.tmdb.org/t/p/original"
    private const val StillUrl = "https://image.tmdb.org/t/p/w500"
    private const val TimeoutMillis = 8_000
    private const val YearLength = 4

    // The code TMDB sometimes uses for a picture that has no language
    private const val NoLanguageCode = "xx"
    private const val LanguageLength = 2
    private const val MovieKind = "movie"

    val isSet: Boolean get() = BuildConfig.TMDB_TOKEN.isNotEmpty()

    // The posters, logos and backdrops of a title in every language, since asking for some hides the others
    fun images(kind: String, id: Int): SiteImages {
        if (!isSet) return SiteImages.None
        return try {
            val json = get("/$kind/$id/images") ?: return SiteImages.None
            SiteImages(artworks(json.optJSONArray("posters")), artworks(json.optJSONArray("logos")), artworks(json.optJSONArray("backdrops")))
        } catch (_: Exception) {
            SiteImages.Failed
        }
    }

    // The poster of a season and the day it first aired, none when TMDB does not have the season
    fun seasonArt(id: Int, season: Int): SeasonArtwork {
        if (!isSet) return SeasonArtwork.None
        return try {
            val details = get("/tv/$id/season/$season") ?: return SeasonArtwork.None
            val airDate = details.optString("air_date").takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val posters = artworks(get("/tv/$id/season/$season/images")?.optJSONArray("posters"))
            SeasonArtwork(airDate, posters, Artworks.None, failed = false)
        } catch (_: Exception) {
            SeasonArtwork.Failed
        }
    }

    // The id on YouTube of the trailer of a title, the official ones first, null when the answer failed
    fun trailer(kind: String, id: Int): String? {
        if (!isSet) return null
        return try {
            val list = get("/$kind/$id/videos")?.optJSONArray("results") ?: return ""
            (0 until list.length()).map { list.getJSONObject(it) }
                .filter { it.optString("site") == "YouTube" && it.optString("type") == "Trailer" }
                .sortedWith(compareByDescending<JSONObject> { it.optBoolean("official") }.thenByDescending { it.optString("published_at") })
                .firstOrNull()?.optString("key").orEmpty()
        } catch (_: Exception) {
            null
        }
    }

    // The episodes of a season in a language, empty when there is no season and null when the answer failed
    fun season(id: Int, season: Int, language: String): List<TmdbEpisode>? {
        if (!isSet) return null
        return try {
            val list = get("/tv/$id/season/$season?language=$language")?.optJSONArray("episodes") ?: return emptyList()
            (0 until list.length()).map { list.getJSONObject(it) }.map { episode ->
                TmdbEpisode(
                    number = episode.optInt("episode_number"),
                    airDate = if (episode.isNull("air_date")) null else episode.optString("air_date").takeIf { it.isNotBlank() },
                    name = episode.optString("name"),
                    overview = episode.optString("overview"),
                    vote = episode.optDouble("vote_average", 0.0).toFloat(),
                    votes = episode.optInt("vote_count"),
                    still = if (episode.isNull("still_path")) null else episode.optString("still_path").takeIf { it.isNotBlank() }?.let { StillUrl + it },
                    minutes = episode.optInt("runtime").takeIf { it > 0 }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    // The id of a series on TheTVDB, null when TMDB does not have it or the answer failed. Blocks on the network
    fun tvdbIdOf(tmdbId: Int): Int? {
        if (!isSet) return null
        return try {
            get("/tv/$tmdbId/external_ids")?.optInt("tvdb_id")?.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    // The id of a series on IMDb, null when TMDB does not have it or the answer failed. Blocks on the network
    fun imdbIdOf(tmdbId: Int): String? {
        if (!isSet) return null
        return try {
            get("/tv/$tmdbId/external_ids")?.let { if (it.isNull("imdb_id")) null else it.optString("imdb_id") }?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    // The ratings of every country a title has, a series by one list and a movie by the dates of its release. Blocks on the network
    fun certifications(kind: String, id: Int): Certified {
        if (!isSet) return Certified.None
        return try {
            if (kind == MovieKind) movieCertifications(id) else seriesCertifications(id)
        } catch (_: Exception) {
            Certified.Failed
        }
    }

    // The ratings of the countries of a series
    private fun seriesCertifications(id: Int): Certified {
        val list = get("/tv/$id/content_ratings")?.optJSONArray("results") ?: return Certified.None
        return Certified(
            byCountry = (0 until list.length()).mapNotNull { index ->
                val item = list.getJSONObject(index)
                item.optString("rating").trim().takeIf { it.isNotEmpty() }?.let { item.getString("iso_3166_1") to it }
            }.toMap(),
            failed = false
        )
    }

    // The ratings of the countries of a movie
    private fun movieCertifications(id: Int): Certified {
        val list = get("/movie/$id/release_dates")?.optJSONArray("results") ?: return Certified.None
        return Certified(
            byCountry = (0 until list.length()).mapNotNull { index ->
                val item = list.getJSONObject(index)
                val dates = item.optJSONArray("release_dates") ?: return@mapNotNull null

                // A country has one for each release, the first that is filled is the one
                (0 until dates.length()).firstNotNullOfOrNull { dates.getJSONObject(it).optString("certification").trim().takeIf { c -> c.isNotEmpty() } }
                    ?.let { item.getString("iso_3166_1") to it }
            }.toMap(),
            failed = false
        )
    }

    // Reads the pictures of a list
    private fun artworks(list: JSONArray?): Artworks {
        if (list == null) return Artworks.None
        return Artworks(
            list = (0 until list.length()).mapNotNull { index ->
                val image = list.getJSONObject(index)
                val path = if (image.isNull("file_path")) null else image.getString("file_path")
                path?.let {
                    Artwork(
                        url = ImageUrl + it,
                        width = image.optInt("width"),
                        height = image.optInt("height"),
                        language = if (image.isNull("iso_639_1")) {
                            null
                        } else {
                            image.getString("iso_639_1").takeIf { code -> code.length == LanguageLength && code != NoLanguageCode }
                        },
                        score = image.optDouble("vote_average", 0.0)
                    )
                }
            },
            failed = false
        )
    }

    // The results of a search by name, empty when there is none or the answer failed
    fun search(kind: String, name: String): List<TmdbHit> {
        if (!isSet) return emptyList()
        return try {
            val json = get("/search/$kind?include_adult=false&query=${URLEncoder.encode(name, "UTF-8")}") ?: return emptyList()
            val list = json.optJSONArray("results") ?: return emptyList()
            (0 until list.length()).map { list.getJSONObject(it) }.map { hit ->
                TmdbHit(
                    id = hit.getInt("id"),
                    // A series has a name and a movie has a title, each with the original one
                    names = listOf("name", "title", "original_name", "original_title")
                        .mapNotNull { key -> if (hit.isNull(key)) null else hit.optString(key).takeIf { it.isNotBlank() } },
                    year = listOf("first_air_date", "release_date")
                        .firstNotNullOfOrNull { key -> if (hit.isNull(key)) null else hit.optString(key).take(YearLength).toIntOrNull() },
                    votes = hit.optInt("vote_count")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Null when TMDB does not know what was asked, anything else that is not an answer is an error
    private fun get(path: String): JSONObject? = Gates.Tmdb.run { getOnce(path) }

    // Asks TMDB once and reads the answer
    private fun getOnce(path: String): JSONObject? {
        val connection = URL(ApiUrl + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.TMDB_TOKEN}")
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).readText())
                HttpURLConnection.HTTP_NOT_FOUND -> null
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("TMDB answered ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
}
