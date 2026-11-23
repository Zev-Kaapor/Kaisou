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
    suspend fun heroPool(token: String?, refresh: Boolean = false): List<AniMedia>? = withContext(Dispatchers.IO) {
        val season = AniSeason.of(LocalDate.now())
        val key = "${season.name} ${season.year} ${if (token != null) "user" else "guest"}"
        if (!refresh) AppStore.get(PoolSpace, key, PoolMillis)?.let { kept -> parsePool(kept)?.let { return@withContext it } }
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

    // The shelves of the Home come in two parts. The top has what the page waits for, what the friends did and the lists of what the user is
    // in the middle of. It is always asked for, since the friends do things all the time, and kept only for when AniList does not
    // answer, then the last one kept is used, however old. Null when there is none
    suspend fun topShelves(userId: Int, token: String?): AniShelves? =
        fetchShelves(TopSpace, userId.toString(), TopQuery, JSONObject().put("userId", userId), token)

    // The part under the top: the episodes that aired, the lists of what the user plans and the shelves of discovery. What was kept of it
    // is shown while the new one comes, see keptBelow
    suspend fun belowShelves(userId: Int, token: String?): AniShelves? {
        val now = System.currentTimeMillis() / MillisPerSecond
        val season = AniSeason.of(LocalDate.now())
        val next = season.next()
        val variables = JSONObject()
            .put("userId", userId).put("from", now - NewEpisodesSeconds).put("to", now)
            .put("season", season.name).put("year", season.year).put("nextSeason", next.name).put("nextYear", next.year)
        return fetchShelves(BelowSpace, userId.toString(), BelowQuery, variables, token)
    }

    private suspend fun fetchShelves(space: String, key: String, query: String, variables: JSONObject, token: String?): AniShelves? =
        withContext(Dispatchers.IO) {
            val fresh = try {
                post(query, variables, token)?.toString()
            } catch (_: Exception) {
                null
            }
            if (fresh != null) {
                AppStore.put(space, key, fresh)
                return@withContext parseShelves(fresh)
            }
            AppStore.getAny(space, key)?.let(::parseShelves)
        }

    // What was kept of what is lower on the page, however old, so the page can show it at once while the new one comes. Null when nothing
    // was kept
    suspend fun keptBelow(userId: Int): AniShelves? = withContext(Dispatchers.IO) { AppStore.getAny(BelowSpace, userId.toString())?.let(::parseShelves) }

    suspend fun keptRecommended(userId: Int): AniRecommended? =
        withContext(Dispatchers.IO) { AppStore.getAny(RecommendedSpace, userId.toString())?.let(::parseRecommended) }

    suspend fun keptSequels(userId: Int): List<AniCard>? =
        withContext(Dispatchers.IO) { AppStore.getAny(SequelsSpace, userId.toString())?.let(::parseSequels) }

    // What is recommended for the user, asked for every time and kept like the shelves. It is asked apart because it is a heavy question, and if it fails the
    // shelves still come
    suspend fun recommended(userId: Int, token: String?): AniRecommended? = withContext(Dispatchers.IO) {
        val key = userId.toString()
        val fresh = try {
            post(RecommendedQuery, JSONObject().put("userId", userId), token)?.toString()
        } catch (_: Exception) {
            null
        }
        if (fresh != null) {
            AppStore.put(RecommendedSpace, key, fresh)
            return@withContext parseRecommended(fresh)
        }
        AppStore.getAny(RecommendedSpace, key)?.let(::parseRecommended)
    }

    // The sequels of what the user finished that they do not have on their list and that already came out. The titles to look for are
    // found among everything they finished, which is kept for hours, and are asked for in one question with a part for each batch, and the
    // answer is kept like the shelves. Asked apart, so if it fails the shelves still come
    suspend fun missingSequels(userId: Int, token: String?): List<AniCard>? = withContext(Dispatchers.IO) {
        val key = userId.toString()
        val fresh = try {
            val ids = sequelIds(key, userId, token)
            if (ids == null) {
                null
            } else {
                val batches = ids.chunked(SequelBatch).take(MaxSequelBatches)
                val media = JSONArray()
                val data = if (batches.isEmpty()) null else post(sequelsQuery(batches), JSONObject(), token)
                // Each batch is a part of the answer, one that failed is only a part left out, and none that came means nothing to show
                data?.keys()?.forEach { alias ->
                    val items = data.optJSONObject(alias)?.optJSONArray("media") ?: return@forEach
                    for (index in 0 until items.length()) media.put(items.getJSONObject(index))
                }
                if (batches.isNotEmpty() && media.length() == 0) null else JSONObject().put("sequels", JSONObject().put("media", media)).toString()
            }
        } catch (_: Exception) {
            null
        }
        if (fresh != null) {
            AppStore.put(SequelsSpace, key, fresh)
            return@withContext parseSequels(fresh)
        }
        AppStore.getAny(SequelsSpace, key)?.let(::parseSequels)
    }

    // The ids of the sequels of what the user finished. They change only when the user finishes something, so they are kept for hours and
    // the long question about everything they finished is not asked at every start
    private fun sequelIds(key: String, userId: Int, token: String?): List<Int>? {
        AppStore.get(SequelIdsSpace, key, SequelIdsMillis)?.let { kept -> parseIds(kept)?.let { return it } }
        val completed = post(CompletedRelationsQuery, JSONObject().put("userId", userId), token) ?: return null
        val ids = AniShelvesParser.sequelIds(completed)
        AppStore.put(SequelIdsSpace, key, JSONArray(ids).toString())
        return ids
    }

    private fun parseIds(text: String): List<Int>? =
        try {
            JSONArray(text).let { array -> (0 until array.length()).map { array.getInt(it) } }
        } catch (_: Exception) {
            null
        }

    private fun parseSequels(text: String): List<AniCard>? =
        try {
            AniShelvesParser.parseSequels(JSONObject(text))
        } catch (_: Exception) {
            null
        }

    // Likes an activity, or takes the like back if it was already given, and says how it stands now. This changes the account of the
    // user, so it is only called when they touch the heart. Null when AniList did not do it
    suspend fun toggleLike(id: Int, reply: Boolean, token: String?): AniLike? = toggleLikeOf(id, if (reply) "ACTIVITY_REPLY" else "ACTIVITY", token)

    // The same for what AniList calls a kind of thing that can be liked, as a thread or a comment of a thread
    suspend fun toggleLikeOf(id: Int, type: String, token: String?): AniLike? = withContext(Dispatchers.IO) {
        try {
            val variables = JSONObject().put("id", id).put("type", type)
            post(ToggleLikeQuery, variables, token)?.optJSONObject("ToggleLikeV2")
                ?.let { AniLike(isLiked = it.optBoolean("isLiked"), likeCount = it.optInt("likeCount")) }
        } catch (_: Exception) {
            null
        }
    }

    // Follows an activity to know of its new comments, or stops following it, and says whether it is followed now, null when AniList did not do it
    suspend fun toggleSubscription(activityId: Int, subscribe: Boolean, token: String?): Boolean? = withContext(Dispatchers.IO) {
        try {
            post(SubscribeActivityQuery, JSONObject().put("id", activityId).put("subscribe", subscribe), token)?.optJSONObject("ToggleActivitySubscription")
                ?.optBoolean("isSubscribed")
        } catch (_: Exception) {
            null
        }
    }

    // Deletes an activity of the user from AniList for good, so it is only called after they confirm. True when AniList did it
    suspend fun deleteActivity(activityId: Int, token: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            post(DeleteActivityQuery, JSONObject().put("id", activityId), token)?.optJSONObject("DeleteActivity")?.optBoolean("deleted") == true
        } catch (_: Exception) {
            false
        }
    }

    // Posts a status, a text on the activity of the user, as the user. This changes their account, so it is only called when they
    // press the button to post. True when AniList took it
    suspend fun postStatus(text: String, token: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            post(SaveStatusQuery, JSONObject().put("text", text), token)?.optJSONObject("SaveTextActivity")?.has("id") == true
        } catch (_: Exception) {
            false
        }
    }

    // The comments of an activity, null when AniList did not answer
    suspend fun replies(activityId: Int, token: String?): List<AniReply>? = withContext(Dispatchers.IO) {
        try {
            val items = post(RepliesQuery, JSONObject().put("id", activityId), token)?.optJSONObject("Page")?.optJSONArray("activityReplies")
            items?.let { list -> (0 until list.length()).mapNotNull { AniShelvesParser.reply(list.getJSONObject(it)) } }
        } catch (_: Exception) {
            null
        }
    }

    // Deletes a comment of the user for good, so it is only called after they confirm. True when AniList did it
    suspend fun deleteReply(replyId: Int, token: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            post(DeleteReplyQuery, JSONObject().put("id", replyId), token)?.optJSONObject("DeleteActivityReply")?.optBoolean("deleted") == true
        } catch (_: Exception) {
            false
        }
    }

    // Changes the text of a comment of the user, and gives it as AniList kept it. Null when AniList did not take it
    suspend fun editReply(activityId: Int, replyId: Int, text: String, token: String?): AniReply? = withContext(Dispatchers.IO) {
        try {
            val variables = JSONObject().put("id", replyId).put("activity", activityId).put("text", text)
            post(EditReplyQuery, variables, token)?.optJSONObject("SaveActivityReply")?.let(AniShelvesParser::reply)
        } catch (_: Exception) {
            null
        }
    }

    // Writes a comment on an activity as the user, and gives it as AniList kept it. Null when AniList did not take it
    suspend fun reply(activityId: Int, text: String, token: String?): AniReply? = withContext(Dispatchers.IO) {
        try {
            post(SaveReplyQuery, JSONObject().put("id", activityId).put("text", text), token)?.optJSONObject("SaveActivityReply")
                ?.let(AniShelvesParser::reply)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRecommended(text: String): AniRecommended? =
        try {
            AniShelvesParser.parseRecommended(JSONObject(text))
        } catch (_: Exception) {
            null
        }

    private fun parseShelves(text: String): AniShelves? =
        try {
            AniShelvesParser.parse(JSONObject(text))
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

    // Any question to AniList, for the screens that own their query: the data of the answer, null when there was none or it failed
    suspend fun query(query: String, variables: JSONObject, token: String?): JSONObject? = withContext(Dispatchers.IO) {
        try {
            post(query, variables, token)
        } catch (_: Exception) {
            null
        }
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
            // What AniList says is always asked again from it, never taken from a copy on the way
            connection.setRequestProperty("Cache-Control", "no-cache")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.outputStream.use {
                it.write(JSONObject().put("query", query).put("variables", variables).toString().toByteArray())
            }
            if (connection.responseCode == TooManyRequests.Code) throw TooManyRequests.of(connection)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            // AniList says how much of its limit is left, which is 30 a minute when it is slow and 90 when it is not. When little is left the next
            // requests wait for it to come back, so a burst of screens does not get turned away
            val left = connection.getHeaderField("X-RateLimit-Remaining")?.trim()?.toIntOrNull()
            val reset = connection.getHeaderField("X-RateLimit-Reset")?.trim()?.toLongOrNull()
            if (left != null && reset != null && left <= RateReserve) Gates.AniList.restUntil(reset * MillisPerSecond)
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
        // How many requests of the limit are kept for what comes after, when the limit says how many are left
        const val RateReserve = 2
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

        // Where the two parts of the shelves are kept on the disk, and how far back the new episodes go
        const val TopSpace = "top2"
        const val BelowSpace = "below2"
        const val MillisPerSecond = 1000L
        const val NewEpisodesSeconds = 48L * 60L * 60L

        // What a card of a shelf needs of a title, and how many of each list and of the new episodes come
        const val ShelfCard = AniShelvesParser.CardFields
        const val ShelfSize = 20
        const val AiredSize = 50
        const val ActivitySize = 50
        const val MineSize = 25
        // An activity of a list or of a text, with who did it
        const val ActivityFields = "__typename ... on ListActivity { id createdAt likeCount replyCount isLiked status progress user { id name avatar { large } } media { $ShelfCard } } " +
            "... on TextActivity { id createdAt likeCount replyCount isLiked text(asHtml: false) user { id name avatar { large } } }"
        // Likes and comments on the activities
        const val ReplyFields = "id text(asHtml: false) createdAt likeCount isLiked user { id name avatar { large } }"
        const val ToggleLikeQuery = "mutation (${'$'}id: Int, ${'$'}type: LikeableType) { ToggleLikeV2(id: ${'$'}id, type: ${'$'}type) { " +
            "... on ListActivity { isLiked likeCount } ... on TextActivity { isLiked likeCount } ... on ActivityReply { isLiked likeCount } ... on Thread { isLiked likeCount } ... on ThreadComment { isLiked likeCount } } }"
        const val DeleteReplyQuery = "mutation (${'$'}id: Int) { DeleteActivityReply(id: ${'$'}id) { deleted } }"
        const val EditReplyQuery = "mutation (${'$'}id: Int, ${'$'}activity: Int, ${'$'}text: String) { " +
            "SaveActivityReply(id: ${'$'}id, activityId: ${'$'}activity, text: ${'$'}text) { $ReplyFields } }"
        const val SubscribeActivityQuery = "mutation (${'$'}id: Int, ${'$'}subscribe: Boolean) { ToggleActivitySubscription(activityId: ${'$'}id, subscribe: ${'$'}subscribe) { " +
            "... on ListActivity { isSubscribed } ... on TextActivity { isSubscribed } ... on MessageActivity { isSubscribed } } }"
        const val DeleteActivityQuery = "mutation (${'$'}id: Int) { DeleteActivity(id: ${'$'}id) { deleted } }"
        const val SaveStatusQuery = "mutation (${'$'}text: String) { SaveTextActivity(text: ${'$'}text) { id } }"
        const val RepliesQuery = "query (${'$'}id: Int) { Page(perPage: 50) { activityReplies(activityId: ${'$'}id) { $ReplyFields } } }"
        const val SaveReplyQuery = "mutation (${'$'}id: Int, ${'$'}text: String) { SaveActivityReply(activityId: ${'$'}id, text: ${'$'}text) { $ReplyFields } }"

        // Recommendations: where they are kept, how many come at a time, and how many manga of the user they are taken from
        const val RecommendedSpace = "recommended2"
        const val RecommendationsPage = 50
        const val MangaSeeds = 25
        const val PerMangaSeed = 6
        const val RecommendedCard = "$ShelfCard type mediaListEntry { status }"

        // Sequels: where they are kept, how many titles are asked at a time and how many times at most
        const val SequelsSpace = "sequels2"
        const val SequelIdsSpace = "sequelids2"
        const val SequelIdsMillis = 6L * 60L * 60L * 1000L
        const val SequelBatch = 50
        const val MaxSequelBatches = 4

        // The new titles only count when some people already have them, otherwise the ones just made up fill the shelf
        const val NewMinPopularity = 100

        // A page of titles of the shelves of discovery
        private fun discover(alias: String, arguments: String): String =
            "$alias: Page(perPage: $ShelfSize) { media($arguments, isAdult: false) { $ShelfCard } }"

        // One list of the user, the ones touched last first
        private fun shelfList(alias: String, type: String, statuses: String): String =
            "$alias: MediaListCollection(userId: ${'$'}userId, type: $type, status_in: [$statuses], sort: UPDATED_TIME_DESC, " +
                "chunk: 1, perChunk: $ShelfSize) { lists { entries { progress media { $ShelfCard } } } }"

        // The top of the shelves: what the friends did and the lists of what the user is in the middle of
        val TopQuery = """
            query (${'$'}userId: Int) {
              ${shelfList("watching", "ANIME", "CURRENT, REPEATING")}
              ${shelfList("reading", "MANGA", "CURRENT, REPEATING")}
              activity: Page(perPage: $ActivitySize) {
                activities(isFollowing: true, type_in: [TEXT, ANIME_LIST, MANGA_LIST, MEDIA_LIST], sort: ID_DESC) { $ActivityFields }
              }
              mine: Page(perPage: $MineSize) {
                activities(userId: ${'$'}userId, type_in: [TEXT, ANIME_LIST, MANGA_LIST, MEDIA_LIST], sort: ID_DESC) { $ActivityFields }
              }
            }
        """

        // The part under the top: the lists of what is planned, the episodes that aired and the shelves of discovery
        val BelowQuery = """
            query (
              ${'$'}userId: Int, ${'$'}from: Int, ${'$'}to: Int,
              ${'$'}season: MediaSeason, ${'$'}year: Int, ${'$'}nextSeason: MediaSeason, ${'$'}nextYear: Int
            ) {
              ${shelfList("plannedAnime", "ANIME", "PLANNING")}
              ${shelfList("plannedManga", "MANGA", "PLANNING")}
              airing: Page(perPage: $AiredSize) {
                airingSchedules(airingAt_greater: ${'$'}from, airingAt_lesser: ${'$'}to, sort: TIME_DESC) { airingAt episode media { $ShelfCard } }
              }
              ${discover("trending", "type: ANIME, sort: TRENDING_DESC")}
              ${discover("trendingManga", "type: MANGA, sort: TRENDING_DESC")}
              ${discover("popular", "type: ANIME, season: ${'$'}season, seasonYear: ${'$'}year, sort: POPULARITY_DESC")}
              ${discover("upcoming", "type: ANIME, season: ${'$'}nextSeason, seasonYear: ${'$'}nextYear, sort: POPULARITY_DESC")}
              ${discover("newAnime", "type: ANIME, sort: ID_DESC, popularity_greater: $NewMinPopularity")}
              ${discover("newManga", "type: MANGA, sort: ID_DESC, popularity_greater: $NewMinPopularity")}
            }
        """

        // Recommendations of the titles on the list of the user, the best rated and the newest, and of the manga read last
        val RecommendedQuery = """
            query (${'$'}userId: Int) {
              byRating: Page(perPage: $RecommendationsPage) {
                recommendations(sort: RATING_DESC, onList: true) { mediaRecommendation { $RecommendedCard } }
              }
              byNew: Page(perPage: $RecommendationsPage) {
                recommendations(sort: ID_DESC, onList: true) { mediaRecommendation { $RecommendedCard } }
              }
              mangaSeeds: MediaListCollection(userId: ${'$'}userId, type: MANGA, status_in: [CURRENT, COMPLETED, REPEATING], sort: UPDATED_TIME_DESC, chunk: 1, perChunk: $MangaSeeds) {
                lists { entries { media { recommendations(sort: RATING_DESC, perPage: $PerMangaSeed) { nodes { mediaRecommendation { $RecommendedCard } } } } } }
              }
            }
        """

        // The sequels of everything the user finished, the whole list is looked at since the old titles are the ones with sequels
        val CompletedRelationsQuery = """
            query (${'$'}userId: Int) {
              completed: MediaListCollection(userId: ${'$'}userId, type: ANIME, status: COMPLETED, sort: UPDATED_TIME_DESC, chunk: 1, perChunk: 500) {
                lists { entries { media { id relations { edges { relationType(version: 2) node { id type } } } } } }
              }
            }
        """

        // Among the batches of titles, the ones that came out and are not on the list of the user, each batch a part of one question
        private fun sequelsQuery(batches: List<List<Int>>): String =
            "query { " + batches.mapIndexed { index, ids ->
                "b$index: Page(perPage: $SequelBatch) { media(id_in: [${ids.joinToString(",")}], type: ANIME, status_in: [RELEASING, FINISHED], " +
                    "onList: false, isAdult: false) { $ShelfCard } }"
            }.joinToString(" ") + " }"

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
