package com.kaisou.app.core.anilist

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Java networking and time
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

// A title as AniList describes it, the fields that were not asked for come empty
data class AniMedia(
    val id: Int,
    // Id of the title on MyAnimeList, null when it has none
    val malId: Int?,
    val title: String,
    // Every name the title goes by, the English one first, to look it up on other sites
    val names: List<String>,
    // ANIME or MANGA
    val type: String?,
    val format: String?,
    val genres: List<String>,
    val year: Int?,
    val description: String?,
    // How many episodes or chapters it has, null while it is not known
    val episodes: Int?,
    val chapters: Int?,
    // Next episode that has not aired yet and the moment it airs, null when the title is not airing
    val nextEpisode: Int?,
    val nextAiringAt: Long?,
    // Where the title is on the list of the user, null when it is not on it
    val listStatus: String?,
    val listProgress: Int?,
    // Average score from 0 to 100
    val meanScore: Int?,
    // Main color of the cover, like #e4a15d
    val color: String?,
    val cover: String?,
    val banner: String?
)

// An episode that aired, with the moment it did in seconds
data class AniAiring(
    val episode: Int,
    val airingAt: Long,
    val media: AniMedia
)

// What AniList shows to everybody at the top of the Home: the titles of the season people follow the most, from which
// the featured ones are drawn, the ones rising on the whole site and the episodes that aired last
data class AniHome(
    val pool: List<AniMedia>,
    val trending: List<AniMedia>,
    val airing: List<AniAiring>
)

// What AniList shows to everybody lower on the Home
data class AniDiscover(
    val nextSeason: List<AniMedia>,
    val newAnime: List<AniMedia>,
    val newManga: List<AniMedia>
)

// A title on the list of the user, with how far they are
data class AniEntry(
    val progress: Int,
    val updatedAt: Long,
    val media: AniMedia
)

// What a person the user follows did on their list
data class AniActivity(
    val userId: Int,
    val userName: String,
    val avatar: String?,
    // What happened in the words of AniList, like "watched episode" or "completed"
    val status: String,
    // The episode or chapter, or the range of them, null for the activities that have none
    val progress: String?,
    val media: AniMedia
)

// What AniList shows only to the user that is signed in, at the top of the Home
data class AniPersonal(
    val watching: List<AniEntry>,
    val reading: List<AniEntry>,
    val plannedAnime: List<AniEntry>,
    val plannedManga: List<AniEntry>,
    val activities: List<AniActivity>
)

// The user that is signed in
data class AniViewer(
    val id: Int,
    val name: String,
    val avatar: String?,
    val banner: String?,
    // Whether the account asks to see adult content
    val adult: Boolean
)

// What came of asking AniList who the token belongs to: the user, the token being refused, or no answer at all
sealed interface ViewerResult {
    data class Found(val viewer: AniViewer) : ViewerResult
    data object Rejected : ViewerResult
    data object Unreachable : ViewerResult
}

// Queries of the AniList GraphQL API: the public ones need no account, the personal ones go with the token.
// Every one of them returns null when AniList cannot be reached or refuses, so a shelf that fails does not take the others
class AniListClient {

    suspend fun home(): AniHome? = asked {
        val season = AniSeason.of(LocalDate.now())
        val variables = JSONObject()
            .put("now", System.currentTimeMillis() / MillisPerSecond)
            .put("season", season.name)
            .put("seasonYear", season.year)
        val data = post(HomeQuery, variables, token = null) ?: return@asked null
        AniHome(
            pool = readMedia(data.getJSONObject("pool").getJSONArray("media")),
            trending = readMedia(data.getJSONObject("trending").getJSONArray("media")),
            airing = readAiring(data.getJSONObject("airing").getJSONArray("airingSchedules"))
        )
    }

    suspend fun discover(): AniDiscover? = asked {
        val next = AniSeason.of(LocalDate.now()).next()
        val variables = JSONObject().put("season", next.name).put("seasonYear", next.year)
        val data = post(DiscoverQuery, variables, token = null) ?: return@asked null
        AniDiscover(
            nextSeason = readMedia(data.getJSONObject("nextSeason").getJSONArray("media")),
            newAnime = readMedia(data.getJSONObject("newAnime").getJSONArray("media")),
            newManga = readMedia(data.getJSONObject("newManga").getJSONArray("media"))
        )
    }

    suspend fun personal(token: String, userId: Int): AniPersonal? = asked {
        val data = post(PersonalQuery, JSONObject().put("userId", userId), token) ?: return@asked null
        AniPersonal(
            watching = readEntries(data.getJSONObject("animeNow")),
            reading = readEntries(data.getJSONObject("mangaNow")),
            plannedAnime = readEntries(data.getJSONObject("animePlanned")),
            plannedManga = readEntries(data.getJSONObject("mangaPlanned")),
            activities = readActivities(data.getJSONObject("activity").getJSONArray("activities"))
        )
    }

    // Titles that people recommend after the ones on the list of the user, the ones already on it left out, best score first
    suspend fun recommended(token: String, userId: Int): List<AniMedia>? = asked {
        val data = post(RecommendedQuery, JSONObject().put("userId", userId), token) ?: return@asked null
        // Anime come from the recommendations of everything on the list. Manga come from the ones of the manga read last,
        // since the list of most users is mostly anime, which would leave almost no room for manga
        val anime = listOf("byRating", "byNew").flatMap { key ->
            val items = data.getJSONObject(key).getJSONArray("recommendations")
            (0 until items.length()).mapNotNull { items.getJSONObject(it).optJSONObject("mediaRecommendation") }
        }
        val lists = data.optJSONObject("mangaSeeds")?.optJSONArray("lists") ?: JSONArray()
        val manga = (0 until lists.length()).flatMap { index ->
            val entries = lists.getJSONObject(index).optJSONArray("entries") ?: JSONArray()
            (0 until entries.length()).flatMap { entry ->
                val nodes = entries.getJSONObject(entry).getJSONObject("media")
                    .optJSONObject("recommendations")?.optJSONArray("nodes") ?: JSONArray()
                (0 until nodes.length()).mapNotNull { nodes.getJSONObject(it).optJSONObject("mediaRecommendation") }
            }
        }
        best(anime, AnimeType) + best(manga, MangaType)
    }

    // The recommended titles of a type that are not on the list of the user yet, the best scored first
    private fun best(found: List<JSONObject>, type: String): List<AniMedia> =
        found.filterNot { it.optBoolean("isAdult") }
            .map { parseMedia(it) }
            .filter { it.type == type && it.listStatus == null }
            .distinctBy { it.id }
            .sortedByDescending { it.meanScore ?: 0 }
            .take(MaxRecommended)

    // The sequels of everything the user finished that are not on their list yet and have already come out, the best
    // scored first. The whole list is looked at, not only what was touched last, since the old titles are the ones with sequels
    suspend fun missingSequels(token: String, userId: Int): List<AniMedia>? = asked {
        val completed = post(CompletedRelationsQuery, JSONObject().put("userId", userId), token) ?: return@asked null
        val lists = completed.getJSONObject("completed").optJSONArray("lists") ?: return@asked emptyList()
        val ids = (0 until lists.length())
            .flatMap { index ->
                val entries = lists.getJSONObject(index).optJSONArray("entries") ?: JSONArray()
                (0 until entries.length()).map { entries.getJSONObject(it) }
            }
            .flatMap { entry ->
                val edges = entry.getJSONObject("media").optJSONObject("relations")?.optJSONArray("edges") ?: JSONArray()
                (0 until edges.length()).map { edges.getJSONObject(it) }
            }
            .filter { it.optString("relationType") == "SEQUEL" && it.optJSONObject("node")?.optString("type") == "ANIME" }
            .map { it.getJSONObject("node").getInt("id") }
            .distinct()
        // The ones on the list of the user are left out by the server, a batch at a time
        ids.chunked(SequelBatchSize)
            .take(MaxSequelBatches)
            .flatMap { batch ->
                val found = post(SequelsQuery, JSONObject().put("ids", JSONArray(batch)), token) ?: return@flatMap emptyList()
                readMedia(found.getJSONObject("Page").getJSONArray("media"))
            }
            .filter { it.listStatus == null }
            .distinctBy { it.id }
            .sortedByDescending { it.meanScore ?: 0 }
            .take(MaxMissing)
    }

    // The user the token belongs to
    suspend fun viewer(token: String): ViewerResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("query", ViewerQuery).toString()
            val answer = postAnswer(body, token)
            when {
                answer.code == HttpURLConnection.HTTP_OK -> {
                    val json = answer.json?.optJSONObject("data")?.optJSONObject("Viewer")
                    if (json == null) ViewerResult.Rejected else ViewerResult.Found(parseViewer(json))
                }
                // AniList answers a token that is not valid with a bad request
                answer.code == HttpURLConnection.HTTP_BAD_REQUEST || answer.code == HttpURLConnection.HTTP_UNAUTHORIZED ->
                    ViewerResult.Rejected
                else -> ViewerResult.Unreachable
            }
        } catch (_: Exception) {
            ViewerResult.Unreachable
        }
    }

    // The season that comes before a title, null when it has none or AniList cannot be reached. Blocks on the network,
    // so it is called from a background thread
    fun prequelOf(id: Int): Int? {
        try {
            val data = post(PrequelQuery, JSONObject().put("id", id), token = null) ?: return null
            val edges = data.getJSONObject("Media").getJSONObject("relations").getJSONArray("edges")
            return (0 until edges.length()).map { edges.getJSONObject(it) }
                .firstOrNull { it.getString("relationType") == "PREQUEL" && it.getJSONObject("node").getString("type") == "ANIME" }
                ?.getJSONObject("node")
                ?.getInt("id")
        } catch (_: Exception) {
            return null
        }
    }

    // Runs a query on the background and gives null when anything goes wrong
    private suspend fun <T> asked(block: () -> T?): T? = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (_: Exception) {
            null
        }
    }

    // The data of a query, null when AniList does not answer with success
    private fun post(query: String, variables: JSONObject, token: String?): JSONObject? {
        val body = JSONObject().put("query", query).put("variables", variables).toString()
        val answer = postAnswer(body, token)
        return if (answer.code == HttpURLConnection.HTTP_OK) answer.json?.optJSONObject("data") else null
    }

    // What AniList answered: the status and the body, which is null when it is not JSON
    private class Answer(val code: Int, val json: JSONObject?)

    private fun postAnswer(body: String, token: String?): Answer {
        val connection = URL(ApiUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            // A refused request carries its explanation in the error stream
            val stream = if (code == HttpURLConnection.HTTP_OK) connection.inputStream else connection.errorStream
            val json = stream?.bufferedReader(Charsets.UTF_8)?.readText()?.let { runCatching { JSONObject(it) }.getOrNull() }
            return Answer(code, json)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseViewer(json: JSONObject): AniViewer =
        AniViewer(
            id = json.getInt("id"),
            name = json.getString("name"),
            avatar = json.optJSONObject("avatar")?.let { stringOrNull(it, "large") },
            banner = stringOrNull(json, "bannerImage"),
            adult = json.optJSONObject("options")?.optBoolean("displayAdultContent") ?: false
        )

    private fun readMedia(array: JSONArray): List<AniMedia> =
        (0 until array.length()).map { array.getJSONObject(it) }
            .filterNot { it.optBoolean("isAdult") }
            .map { parseMedia(it) }

    private fun readAiring(array: JSONArray): List<AniAiring> =
        (0 until array.length()).map { array.getJSONObject(it) }
            .filterNot { it.getJSONObject("media").optBoolean("isAdult") }
            .map { AniAiring(it.getInt("episode"), it.getLong("airingAt"), parseMedia(it.getJSONObject("media"))) }
            // The same title can have aired more than one episode, the newest one is enough
            .distinctBy { it.media.id }

    // The entries of a list, the same title can come in more than one list of the user
    private fun readEntries(collection: JSONObject): List<AniEntry> {
        val lists = collection.optJSONArray("lists") ?: return emptyList()
        return (0 until lists.length())
            .flatMap { index ->
                val entries = lists.getJSONObject(index).optJSONArray("entries") ?: JSONArray()
                (0 until entries.length()).map { entries.getJSONObject(it) }
            }
            .filterNot { it.getJSONObject("media").optBoolean("isAdult") }
            .map { AniEntry(it.optInt("progress"), it.optLong("updatedAt"), parseMedia(it.getJSONObject("media"))) }
            .distinctBy { it.media.id }
            .sortedByDescending { it.updatedAt }
    }

    // The latest thing each person did, the activities come from the newest to the oldest
    private fun readActivities(array: JSONArray): List<AniActivity> =
        (0 until array.length()).map { array.getJSONObject(it) }
            .filter { it.has("user") && it.has("media") && !it.isNull("media") }
            .filterNot { it.getJSONObject("media").optBoolean("isAdult") }
            .map { json ->
                val user = json.getJSONObject("user")
                AniActivity(
                    userId = user.getInt("id"),
                    userName = user.getString("name"),
                    avatar = user.optJSONObject("avatar")?.let { stringOrNull(it, "large") },
                    status = json.optString("status"),
                    progress = stringOrNull(json, "progress"),
                    media = parseMedia(json.getJSONObject("media"))
                )
            }
            .distinctBy { it.userId }
            .take(MaxStories)

    private fun parseMedia(json: JSONObject): AniMedia {
        val title = json.getJSONObject("title")
        val genres = json.optJSONArray("genres")
        val next = json.optJSONObject("nextAiringEpisode")
        val cover = json.optJSONObject("coverImage")
        val entry = json.optJSONObject("mediaListEntry")
        val english = stringOrNull(title, "english")
        val romaji = stringOrNull(title, "romaji")
        return AniMedia(
            id = json.getInt("id"),
            malId = intOrNull(json, "idMal"),
            // The English name is the one people know, the romaji is the fallback
            title = english ?: romaji.orEmpty(),
            names = listOfNotNull(english, romaji).distinct(),
            type = stringOrNull(json, "type"),
            format = stringOrNull(json, "format"),
            genres = genres?.let { list -> (0 until list.length()).map { list.getString(it) } }.orEmpty(),
            year = json.optJSONObject("startDate")?.takeUnless { it.isNull("year") }?.getInt("year"),
            description = stringOrNull(json, "description"),
            episodes = intOrNull(json, "episodes"),
            chapters = intOrNull(json, "chapters"),
            nextEpisode = next?.getInt("episode"),
            nextAiringAt = next?.getLong("airingAt"),
            listStatus = entry?.let { stringOrNull(it, "status") },
            listProgress = entry?.let { intOrNull(it, "progress") },
            meanScore = intOrNull(json, "meanScore"),
            color = cover?.let { stringOrNull(it, "color") },
            cover = cover?.let { stringOrNull(it, "extraLarge") },
            banner = stringOrNull(json, "bannerImage")
        )
    }

    // A value that is null on the server comes as the text "null" with the plain getters, and a field that was not asked
    // for is not there, both count as null here
    private fun stringOrNull(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.getString(key)

    private fun intOrNull(json: JSONObject, key: String): Int? =
        if (json.isNull(key)) null else json.getInt(key)

    private companion object {
        const val ApiUrl = "https://graphql.anilist.co"
        const val TimeoutMillis = 15_000
        const val MillisPerSecond = 1000L
        // How many people of the activity feed are shown, how many recommendations are kept and how the sequels are looked up
        const val MaxStories = 12
        const val MaxRecommended = 30
        // How many of the manga read last give their recommendations, and how many each one gives
        const val MangaSeeds = 25
        const val PerMangaSeed = 6
        const val AnimeType = "ANIME"
        const val MangaType = "MANGA"
        const val SequelBatchSize = 50
        const val MaxSequelBatches = 6
        const val MaxMissing = 30
        // How many titles the pool of the featured ones holds and how many a list brings
        const val PoolSize = 40
        const val ListSize = 30

        // What a card of a shelf needs
        const val LightFields = """
            fragment L on Media {
              id
              idMal
              isAdult
              type
              title { romaji english }
              format
              startDate { year }
              episodes
              chapters
              coverImage { extraLarge color }
              bannerImage
            }
        """

        // What the featured titles need besides that
        const val FullFields = """
            fragment M on Media {
              id
              idMal
              isAdult
              type
              title { romaji english }
              format
              genres
              startDate { year }
              description(asHtml: false)
              episodes
              chapters
              nextAiringEpisode { airingAt episode }
              mediaListEntry { status progress }
              coverImage { extraLarge color }
              bannerImage
            }
        """

        // What a recommendation needs: the score to sort by and whether the user has it
        const val RecommendedFields = """
            fragment R on Media {
              id
              idMal
              isAdult
              type
              title { romaji english }
              format
              startDate { year }
              episodes
              chapters
              meanScore
              mediaListEntry { status }
              coverImage { extraLarge color }
              bannerImage
            }
        """

        // Who the token belongs to
        const val ViewerQuery = """
            query {
              Viewer {
                id
                name
                avatar { large }
                bannerImage
                options { displayAdultContent }
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

        // The pool is the titles of this season that people follow the most, the featured ones are drawn from it. Then what is
        // rising on the whole site and the episodes that aired last
        const val HomeQuery = """
            query (${'$'}now: Int, ${'$'}season: MediaSeason, ${'$'}seasonYear: Int) {
              pool: Page(perPage: $PoolSize) {
                media(type: ANIME, season: ${'$'}season, seasonYear: ${'$'}seasonYear, status_in: [RELEASING, FINISHED], sort: TRENDING_DESC, isAdult: false) { ...M }
              }
              trending: Page(perPage: 20) {
                media(type: ANIME, sort: TRENDING_DESC, isAdult: false) { ...L }
              }
              airing: Page(perPage: 20) {
                airingSchedules(airingAt_lesser: ${'$'}now, sort: TIME_DESC) {
                  episode
                  airingAt
                  media { ...L }
                }
              }
            }
            $FullFields
            $LightFields
        """

        // The next season, and what was added to the catalog last
        const val DiscoverQuery = """
            query (${'$'}season: MediaSeason, ${'$'}seasonYear: Int) {
              nextSeason: Page(perPage: 20) {
                media(type: ANIME, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: POPULARITY_DESC, isAdult: false) { ...L }
              }
              newAnime: Page(perPage: 20) {
                media(type: ANIME, sort: ID_DESC, isAdult: false) { ...L }
              }
              newManga: Page(perPage: 20) {
                media(type: MANGA, sort: ID_DESC, isAdult: false) { ...L }
              }
            }
            $LightFields
        """

        // One list of the user, the ones touched last first
        fun collection(alias: String, type: String, status: String, size: Int) =
            "$alias: MediaListCollection(userId: ${'$'}userId, type: $type, status: $status, sort: UPDATED_TIME_DESC, " +
                "chunk: 1, perChunk: $size) { lists { entries { progress updatedAt media { ...L } } } }"

        // What the user is in the middle of, what they plan to see and what the people they follow did
        val PersonalQuery = """
            query (${'$'}userId: Int) {
              ${collection("animeNow", "ANIME", "CURRENT", ListSize)}
              ${collection("mangaNow", "MANGA", "CURRENT", ListSize)}
              ${collection("animePlanned", "ANIME", "PLANNING", ListSize)}
              ${collection("mangaPlanned", "MANGA", "PLANNING", ListSize)}
              activity: Page(perPage: 40) {
                activities(isFollowing: true, type: MEDIA_LIST, sort: ID_DESC) {
                  ... on ListActivity {
                    status
                    progress
                    user { id name avatar { large } }
                    media { ...L }
                  }
                }
              }
            }
            $LightFields
        """

        // Recommendations made from titles that are on the list of the user, the best rated and the newest
        const val RecommendedQuery = """
            query(${'$'}userId: Int) {
              byRating: Page(perPage: 50) {
                recommendations(sort: RATING_DESC, onList: true) { mediaRecommendation { ...R } }
              }
              byNew: Page(perPage: 50) {
                recommendations(sort: ID_DESC, onList: true) { mediaRecommendation { ...R } }
              }
              mangaSeeds: MediaListCollection(userId: ${'$'}userId, type: MANGA, status_in: [CURRENT, COMPLETED, REPEATING], sort: UPDATED_TIME_DESC, chunk: 1, perChunk: $MangaSeeds) {
                lists { entries { media { recommendations(sort: RATING_DESC, perPage: $PerMangaSeed) { nodes { mediaRecommendation { ...R } } } } } }
              }
            }
            $RecommendedFields
        """

        // The sequels of what the user finished
        const val CompletedRelationsQuery = """
            query (${'$'}userId: Int) {
              completed: MediaListCollection(userId: ${'$'}userId, type: ANIME, status: COMPLETED, sort: UPDATED_TIME_DESC, chunk: 1, perChunk: 500) {
                lists { entries { media { id relations { edges { relationType(version: 2) node { id type } } } } } }
              }
            }
        """

        // The titles that came out among a few, the ones that are not on the list of the user
        const val SequelsQuery = """
            query (${'$'}ids: [Int]) {
              Page(perPage: 50) {
                media(id_in: ${'$'}ids, type: ANIME, status_in: [RELEASING, FINISHED], onList: false) { ...R }
              }
            }
            $RecommendedFields
        """
    }
}
