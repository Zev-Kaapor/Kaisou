package com.kaisou.app.core.anilist

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java networking and time
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

// The user that is signed in
data class AniViewer(
    val id: Int,
    val name: String,
    val avatar: String?,
    // What the account has watched and read so far
    val episodesWatched: Int,
    val chaptersRead: Int,
    // Notifications the user has not seen yet, only known after AniList answered
    val unreadNotifications: Int = 0
)

// A title as AniList describes it, the fields that were not asked for come empty
data class AniMedia(
    val id: Int,
    // The English name is the one people know, the romaji is the fallback
    val title: String,
    // Every name the title goes by, the English one first, to look it up on other sites
    val names: List<String>,
    val format: String?,
    val genres: List<String>,
    val year: Int?,
    // The whole day the title started, null when AniList does not know it all
    val startDate: LocalDate?,
    val description: String?,
    // Moment the next episode airs, null when the title is not airing
    val nextAiringAt: Long?,
    // Where the title is on the list of the user, null when it is not on it
    val listStatus: String?,
    val listProgress: Int?,
    // Main color of the cover, like #e4a15d
    val color: String?,
    val cover: String?,
    // The id of the title on MyAnimeList, and whether it is for adults
    val malId: Int?,
    val adult: Boolean,
    // Whether another title comes before this one in the series, which means it is not the first season
    val hasPrequel: Boolean
)

// What came of asking AniList who the token belongs to: the user, the token being refused, or no answer at all
sealed interface ViewerResult {
    data class Found(val viewer: AniViewer) : ViewerResult
    data object Rejected : ViewerResult
    data object Unreachable : ViewerResult
}

// Talks to the GraphQL API of AniList
class AniListClient {

    // The user the token belongs to
    suspend fun viewer(token: String): ViewerResult = withContext(Dispatchers.IO) {
        try {
            Gates.AniList.run { viewerOnce(token) }
        } catch (_: Exception) {
            ViewerResult.Unreachable
        }
    }

    private fun viewerOnce(token: String): ViewerResult {
        val connection = URL(ApiUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write(JSONObject().put("query", ViewerQuery).toString().toByteArray()) }
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body).optJSONObject("data")?.optJSONObject("Viewer")
                    if (json == null) ViewerResult.Rejected else ViewerResult.Found(parseViewer(json))
                }
                // AniList answers a token that is not valid with a bad request
                HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_UNAUTHORIZED -> ViewerResult.Rejected
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> ViewerResult.Unreachable
            }
        } finally {
            connection.disconnect()
        }
    }

    // The most trending titles of the current season, where the featured ones are drawn from. The token is optional, with it
    // each title says whether the user is in the middle of it. The pool is kept for a while, so opening the app again does not ask
    // for it again, and when AniList does not answer the last one kept is used, however old. Null when there is none
    suspend fun heroPool(token: String?): List<AniMedia>? = withContext(Dispatchers.IO) {
        val season = AniSeason.of(LocalDate.now())
        val key = "${season.name} ${season.year} ${if (token != null) "user" else "guest"}"
        AppStore.get(PoolSpace, key, PoolMillis)?.let { kept -> parsePool(kept)?.let { return@withContext it } }
        val fresh = try {
            val variables = JSONObject().put("season", season.name).put("seasonYear", season.year)
            post(HeroQuery, variables, token)?.getJSONObject("Page")?.getJSONArray("media")?.toString()
        } catch (_: Exception) {
            null
        }
        if (fresh != null) {
            AppStore.put(PoolSpace, key, fresh)
            return@withContext parsePool(fresh)
        }
        AppStore.getAny(PoolSpace, key)?.let(::parsePool)
    }

    private fun parsePool(text: String): List<AniMedia>? =
        try {
            val media = JSONArray(text)
            (0 until media.length())
                .map { media.getJSONObject(it) }
                .filterNot { it.optBoolean("isAdult") }
                .map(::parseMedia)
        } catch (_: Exception) {
            null
        }

    // The season that comes before a title, null when it has none or AniList cannot be reached. Blocks on the network, so it
    // is called from a background thread
    fun prequelOf(id: Int): Int? =
        try {
            val data = post(PrequelQuery, JSONObject().put("id", id), token = null)
            val edges = data?.getJSONObject("Media")?.getJSONObject("relations")?.getJSONArray("edges")
            edges?.let { list ->
                (0 until list.length()).map { list.getJSONObject(it) }
                    .firstOrNull { it.getString("relationType") == "PREQUEL" && it.getJSONObject("node").getString("type") == "ANIME" }
                    ?.getJSONObject("node")
                    ?.getInt("id")
            }
        } catch (_: Exception) {
            null
        }

    // Asks AniList and gives the data of the answer, null when there was none. When AniList asks for rest the request waits for it
    private fun post(query: String, variables: JSONObject, token: String?): JSONObject? = Gates.AniList.run { postOnce(query, variables, token) }

    private fun postOnce(query: String, variables: JSONObject, token: String?): JSONObject? {
        val connection = URL(ApiUrl).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use {
                it.write(JSONObject().put("query", query).put("variables", variables).toString().toByteArray())
            }
            if (connection.responseCode == TooManyRequests.Code) throw TooManyRequests.of(connection)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).optJSONObject("data")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseMedia(json: JSONObject): AniMedia {
        val title = json.getJSONObject("title")
        val next = json.optJSONObject("nextAiringEpisode")
        val entry = json.optJSONObject("mediaListEntry")
        val cover = json.optJSONObject("coverImage")
        val english = stringOrNull(title, "english")
        val romaji = stringOrNull(title, "romaji")
        return AniMedia(
            id = json.getInt("id"),
            title = english ?: romaji.orEmpty(),
            names = listOfNotNull(english, romaji).distinct(),
            format = stringOrNull(json, "format"),
            genres = json.optJSONArray("genres")?.let { list -> (0 until list.length()).map { list.getString(it) } }.orEmpty(),
            year = json.optJSONObject("startDate")?.takeUnless { it.isNull("year") }?.getInt("year"),
            startDate = json.optJSONObject("startDate")?.let(::parseDate),
            description = stringOrNull(json, "description"),
            nextAiringAt = next?.getLong("airingAt"),
            listStatus = entry?.let { stringOrNull(it, "status") },
            listProgress = entry?.takeUnless { it.isNull("progress") }?.getInt("progress"),
            color = cover?.let { stringOrNull(it, "color") },
            cover = cover?.let { stringOrNull(it, "extraLarge") },
            malId = json.takeUnless { it.isNull("idMal") }?.optInt("idMal")?.takeIf { it > 0 },
            adult = json.optBoolean("isAdult"),
            hasPrequel = json.optJSONObject("relations")?.optJSONArray("edges")?.let { edges ->
                (0 until edges.length()).any { edges.getJSONObject(it).optString("relationType") == "PREQUEL" }
            } ?: false
        )
    }

    private fun parseDate(json: JSONObject): LocalDate? =
        if (json.isNull("year") || json.isNull("month") || json.isNull("day")) {
            null
        } else {
            runCatching { LocalDate.of(json.getInt("year"), json.getInt("month"), json.getInt("day")) }.getOrNull()
        }

    // A value that is null on the server comes as the text "null" with the plain getters, and a field that was not asked for
    // is not there, both count as null here
    private fun stringOrNull(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.getString(key)

    private fun parseViewer(json: JSONObject): AniViewer {
        val statistics = json.optJSONObject("statistics")
        return AniViewer(
            id = json.getInt("id"),
            name = json.getString("name"),
            // A value that is null on the server comes as the text "null" with the plain getters
            avatar = json.optJSONObject("avatar")?.takeUnless { it.isNull("large") }?.getString("large"),
            episodesWatched = statistics?.optJSONObject("anime")?.optInt("episodesWatched") ?: 0,
            chaptersRead = statistics?.optJSONObject("manga")?.optInt("chaptersRead") ?: 0,
            unreadNotifications = json.optInt("unreadNotificationCount")
        )
    }

    private companion object {
        const val ApiUrl = "https://graphql.anilist.co"
        const val TimeoutMillis = 15_000
        // How many titles the pool of the featured ones holds, where it is kept on the disk and for how long
        const val PoolSize = 40
        const val PoolSpace = "pool"
        const val PoolMillis = 30L * 60L * 1000L

        // What the featured titles need to be shown
        const val HeroQuery = """
            query (${'$'}season: MediaSeason, ${'$'}seasonYear: Int) {
              Page(perPage: $PoolSize) {
                media(type: ANIME, season: ${'$'}season, seasonYear: ${'$'}seasonYear, status_in: [RELEASING, FINISHED], sort: TRENDING_DESC, isAdult: false) {
                  id
                  idMal
                  isAdult
                  title { romaji english }
                  format
                  genres
                  startDate { year month day }
                  description(asHtml: false)
                  nextAiringEpisode { airingAt }
                  mediaListEntry { status progress }
                  coverImage { extraLarge color }
                  relations { edges { relationType } }
                }
              }
            }
        """

        // The title that comes before another one, to find the series of a season that the other sites do not know yet
        const val PrequelQuery = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                relations { edges { relationType node { id type } } }
              }
            }
        """

        const val ViewerQuery = """
            query {
              Viewer {
                id
                name
                avatar { large }
                unreadNotificationCount
                statistics {
                  anime { episodesWatched }
                  manga { chaptersRead }
                }
              }
            }
        """
    }
}
