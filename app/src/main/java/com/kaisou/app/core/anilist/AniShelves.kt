package com.kaisou.app.core.anilist

// JSON parsing
import org.json.JSONArray
import org.json.JSONObject

// A title as the shelves of the Home need it, a small part of what a title has
data class AniCard(
    val id: Int,
    val title: String,
    // The names it goes by and the year it started, which the other sites find its pictures by
    val names: List<String>,
    val year: Int?,
    // The score from 0 to 100, null when it has none
    val score: Int?,
    val cover: String?,
    val banner: String?,
    // Whether it is out, like RELEASING, then the format, like TV or MANGA, and how many episodes or chapters it has
    val status: String?,
    val format: String?,
    val episodes: Int?,
    val chapters: Int?,
    // How many episodes have aired so far, known only while the title is airing, when it has no total yet
    val aired: Int?,
    // The id on MyAnimeList, which gives the rating of an anime, and whether AniList marks the title for adults
    val malId: Int? = null,
    val adult: Boolean = false,
    // The score the user gave it from 0 to 100, null when it is not on their list or has no score
    val userScore: Int? = null
)

// One list of the user to ask for as a shelf of the Home: the name of its part of the answer, its kind and its state
class AniListAsk(val alias: String, val type: String, val status: String)

// A title on the list of the user and how far the user is in it
data class AniEntry(val card: AniCard, val progress: Int)

// A new episode that aired: the title, which episode it is and when it aired, in seconds
data class AniAiring(val card: AniCard, val episode: Int, val airedAt: Long)

// What a person did lately: moved a title on their list, with its status and progress, or wrote a text
data class AniActivity(
    val id: Int,
    val userId: Int,
    val userName: String,
    val avatar: String?,
    val createdAt: Long,
    val status: String?,
    val progress: String?,
    val text: String?,
    val card: AniCard?,
    // Whether the title is for adults, which the stories hide only when the option to hide them is on
    val adult: Boolean,
    val likeCount: Int,
    val replyCount: Int,
    val isLiked: Boolean,
    // Whether the user follows the activity, to know of its new comments, which only the questions that ask for it say
    val isSubscribed: Boolean = false
)

// A comment on an activity, with the time in seconds
data class AniReply(
    val id: Int,
    val userId: Int,
    val userName: String,
    val avatar: String?,
    val text: String,
    val createdAt: Long,
    val likeCount: Int,
    val isLiked: Boolean
)

// How an activity stands after a like was given or taken back
data class AniLike(val isLiked: Boolean, val likeCount: Int)

// The shelves of the Home as AniList answers them
class AniShelves(
    // What the people the user follows did, the newest first, and what the user did
    val following: List<AniActivity>,
    val mine: List<AniActivity>,
    val watching: List<AniEntry>,
    val reading: List<AniEntry>,
    val plannedAnime: List<AniEntry>,
    val plannedManga: List<AniEntry>,
    val airing: List<AniAiring>,
    // What is on the way up, what is popular this season, what comes next season and what was added lately, no matter the user
    val trending: List<AniCard>,
    val trendingManga: List<AniCard>,
    val popular: List<AniCard>,
    val upcoming: List<AniCard>,
    val newAnime: List<AniCard>,
    val newManga: List<AniCard>
) {
    // The shelves come in parts that bring different lists and leave the others empty, so putting two together is joining each list
    fun merge(other: AniShelves) = AniShelves(
        following = following + other.following,
        mine = mine + other.mine,
        watching = watching + other.watching,
        reading = reading + other.reading,
        plannedAnime = plannedAnime + other.plannedAnime,
        plannedManga = plannedManga + other.plannedManga,
        airing = airing + other.airing,
        trending = trending + other.trending,
        trendingManga = trendingManga + other.trendingManga,
        popular = popular + other.popular,
        upcoming = upcoming + other.upcoming,
        newAnime = newAnime + other.newAnime,
        newManga = newManga + other.newManga
    )

    // The shelves with nothing in them
    companion object {
        val Empty = AniShelves(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
    }
}

// The titles recommended for the user, anime and manga apart
class AniRecommended(val anime: List<AniCard>, val manga: List<AniCard>)

// Reads the answer of the one request that brings every shelf
object AniShelvesParser {
    // What a card needs of a title, asked of AniList
    const val CardFields = "id idMal isAdult title { romaji english } status format episodes chapters nextAiringEpisode { episode } startDate { year } averageScore meanScore bannerImage coverImage { extraLarge large } mediaListEntry { userScore: score(format: POINT_100) }"

    // How many recommendations are kept
    private const val RecommendedSize = 20

    // What is recommended after the list: anime from the whole list, manga from the last read, with no repeats or adult titles
    fun parseRecommended(data: JSONObject): AniRecommended {
        val found = mutableListOf<JSONObject>()
        for (key in listOf("byRating", "byNew")) {
            val items = data.optJSONObject(key)?.optJSONArray("recommendations") ?: continue
            for (index in 0 until items.length()) items.getJSONObject(index).optJSONObject("mediaRecommendation")?.let(found::add)
        }
        val lists = data.optJSONObject("mangaSeeds")?.optJSONArray("lists")
        for (listIndex in 0 until (lists?.length() ?: 0)) {
            val entries = lists!!.getJSONObject(listIndex).optJSONArray("entries") ?: continue
            for (entryIndex in 0 until entries.length()) {
                val nodes = entries.getJSONObject(entryIndex).optJSONObject("media")?.optJSONObject("recommendations")?.optJSONArray("nodes") ?: continue
                for (nodeIndex in 0 until nodes.length()) nodes.getJSONObject(nodeIndex).optJSONObject("mediaRecommendation")?.let(found::add)
            }
        }
        return AniRecommended(best(found, "ANIME"), best(found, "MANGA"))
    }

    // A comment, null when it has no id
    fun reply(json: JSONObject): AniReply? {
        if (json.isNull("id")) return null
        val user = json.optJSONObject("user")
        return AniReply(
            id = json.getInt("id"),
            userId = user?.optInt("id") ?: 0,
            userName = text(user, "name").orEmpty(),
            avatar = text(user?.optJSONObject("avatar"), "large"),
            text = text(json, "text").orEmpty(),
            createdAt = json.optLong("createdAt"),
            likeCount = json.optInt("likeCount"),
            isLiked = json.optBoolean("isLiked")
        )
    }

    // The ids of the anime that are a sequel of something the user finished, each once
    fun sequelIds(data: JSONObject): List<Int> {
        val lists = data.optJSONObject("completed")?.optJSONArray("lists") ?: return emptyList()
        val ids = mutableListOf<Int>()
        for (listIndex in 0 until lists.length()) {
            val entries = lists.getJSONObject(listIndex).optJSONArray("entries") ?: continue
            for (entryIndex in 0 until entries.length()) {
                val edges = entries.getJSONObject(entryIndex).optJSONObject("media")?.optJSONObject("relations")?.optJSONArray("edges") ?: continue
                for (edgeIndex in 0 until edges.length()) {
                    val edge = edges.getJSONObject(edgeIndex)
                    val node = edge.optJSONObject("node") ?: continue
                    if (edge.optString("relationType") == "SEQUEL" && node.optString("type") == "ANIME" && !node.isNull("id")) ids += node.getInt("id")
                }
            }
        }
        return ids.distinct()
    }

    // The sequels found, the best scored first
    fun parseSequels(data: JSONObject): List<AniCard> = cards(data, "sequels").sortedByDescending { it.score ?: 0 }.take(RecommendedSize)

    // The best scored recommendations of a type, without adult titles and the ones already on the list
    private fun best(found: List<JSONObject>, type: String): List<AniCard> =
        found
            .filter { it.optString("type") == type && !it.optBoolean("isAdult") && it.isNull("mediaListEntry") }
            .mapNotNull { card(it) }
            .distinctBy { it.id }
            .sortedByDescending { it.score ?: 0 }
            .take(RecommendedSize)

    // Reads the shelves of the Home from the answer
    fun parse(data: JSONObject): AniShelves =
        AniShelves(
            following = activities(data.optJSONObject("activity")?.optJSONArray("activities")),
            mine = activities(data.optJSONObject("mine")?.optJSONArray("activities")),
            watching = entries(data.optJSONObject("watching")),
            reading = entries(data.optJSONObject("reading")),
            plannedAnime = entries(data.optJSONObject("plannedAnime")),
            plannedManga = entries(data.optJSONObject("plannedManga")),
            airing = airing(data.optJSONObject("airing")?.optJSONArray("airingSchedules")),
            trending = cards(data, "trending"),
            trendingManga = cards(data, "trendingManga"),
            popular = cards(data, "popular"),
            upcoming = cards(data, "upcoming"),
            newAnime = cards(data, "newAnime"),
            newManga = cards(data, "newManga")
        )

    // The activities of a list or of a text, the private messages and the ones with no user left out
    fun activities(items: JSONArray?): List<AniActivity> {
        if (items == null) return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.getJSONObject(index)
            val typename = item.optString("__typename")
            if ((typename != "ListActivity" && typename != "TextActivity") || item.isNull("id")) return@mapNotNull null
            val user = item.optJSONObject("user")?.takeUnless { it.isNull("id") } ?: return@mapNotNull null
            val isList = typename == "ListActivity"
            val media = item.optJSONObject("media")
            val card = if (isList) (card(media) ?: return@mapNotNull null) else null
            val text = if (isList) null else (text(item, "text") ?: return@mapNotNull null)
            AniActivity(
                id = item.getInt("id"),
                userId = user.getInt("id"),
                userName = text(user, "name").orEmpty(),
                avatar = text(user.optJSONObject("avatar"), "large"),
                createdAt = item.optLong("createdAt"),
                status = text(item, "status"),
                progress = text(item, "progress"),
                text = text,
                card = card,
                adult = media?.optBoolean("isAdult") == true,
                likeCount = item.optInt("likeCount"),
                replyCount = item.optInt("replyCount"),
                isLiked = item.optBoolean("isLiked"),
                isSubscribed = item.optBoolean("isSubscribed")
            )
        }
    }

    // The entries of a list of the user, in the order AniList sent them
    fun entries(collection: JSONObject?): List<AniEntry> {
        val lists = collection?.optJSONArray("lists") ?: return emptyList()
        return (0 until lists.length()).flatMap { index ->
            val entries = lists.getJSONObject(index).optJSONArray("entries") ?: JSONArray()
            (0 until entries.length()).mapNotNull { position ->
                val entry = entries.getJSONObject(position)
                card(entry.optJSONObject("media"))?.let { AniEntry(it, entry.optInt("progress")) }
            }
        }
    }

    // The episodes that aired, a title once with its newest episode, and the ones for adults left out
    private fun airing(schedules: JSONArray?): List<AniAiring> {
        if (schedules == null) return emptyList()
        return (0 until schedules.length())
            .map { schedules.getJSONObject(it) }
            .filterNot { it.optJSONObject("media")?.optBoolean("isAdult") == true }
            .mapNotNull { item -> card(item.optJSONObject("media"))?.let { AniAiring(it, item.optInt("episode"), item.optLong("airingAt")) } }
            .distinctBy { it.card.id }
    }

    // The titles of a page of media
    private fun cards(data: JSONObject, key: String): List<AniCard> {
        val media = data.optJSONObject(key)?.optJSONArray("media") ?: return emptyList()
        return (0 until media.length()).mapNotNull { card(media.getJSONObject(it)) }
    }

    // Reads a card of a title, null when it has no id
    fun card(media: JSONObject?): AniCard? {
        if (media == null || media.isNull("id")) return null
        val title = media.optJSONObject("title")
        val cover = media.optJSONObject("coverImage")
        return AniCard(
            id = media.getInt("id"),
            // The English name is the one people know, the romaji is the fallback
            title = text(title, "english") ?: text(title, "romaji").orEmpty(),
            names = listOfNotNull(text(title, "english"), text(title, "romaji")).distinct(),
            year = media.optJSONObject("startDate")?.let { number(it, "year") },
            // The weighted score only comes when enough people scored the title, the plain mean of the scores fills the gap
            score = number(media, "averageScore") ?: number(media, "meanScore"),
            cover = text(cover, "extraLarge") ?: text(cover, "large"),
            banner = text(media, "bannerImage"),
            status = text(media, "status"),
            format = text(media, "format"),
            episodes = number(media, "episodes"),
            chapters = number(media, "chapters"),
            aired = media.optJSONObject("nextAiringEpisode")?.let { number(it, "episode") }?.minus(1)?.takeIf { it > 0 },
            malId = number(media, "idMal"),
            adult = media.optBoolean("isAdult"),
            userScore = media.optJSONObject("mediaListEntry")?.let { number(it, "userScore") }
        )
    }

    // Reads a positive number, null when it is missing
    private fun number(json: JSONObject, key: String): Int? = if (json.isNull(key)) null else json.optInt(key).takeIf { it > 0 }

    // Reads a text, null when it is missing or comes as "null"
    private fun text(json: JSONObject?, key: String): String? =
        if (json == null || json.isNull(key)) null else json.getString(key).takeIf { it.isNotBlank() }
}
