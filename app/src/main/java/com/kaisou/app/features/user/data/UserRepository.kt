package com.kaisou.app.features.user.data

// Android framework
import android.content.Context
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java time
import java.time.Instant
import java.time.ZoneOffset
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AboutSnippet
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.anilist.AniShelvesParser
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.data.HomeMapper
import com.kaisou.app.features.profile.data.ProfileKind
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.ListChanges
import com.kaisou.app.features.title.data.TitlePerson

// Asks AniList for the page of a person and turns the answer into what the screen shows
class UserRepository(context: Context) {
    private val client = AniListClient()
    private val mapper = HomeMapper(context.resources)
    private val genreNames by lazy { context.resources.getStringArray(R.array.genre_keys).zip(context.resources.getStringArray(R.array.genre_names)).toMap() }

    // The page of the person by their name, null when AniList could not be reached or does not know them
    suspend fun user(name: String): UserUi? {
        pages.get(pageKey(name))?.let { return it.value }
        val root = client.query(UserQuery, JSONObject().put("name", name), AniListSession.token) ?: return null
        val json = root.optJSONObject("User") ?: return null
        val id = json.optInt("id").takeIf { it > 0 } ?: return null
        val statistics = json.optJSONObject("statistics")
        val favourites = json.optJSONObject("favourites")
        val built = UserUi(
            id = id,
            name = text(json, "name") ?: name,
            previousNames = objects(json.optJSONArray("previousNames")).mapNotNull { text(it, "name") },
            joinedYear = json.optLong("createdAt").takeIf { it > 0 }?.let { Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).year },
            avatar = json.optJSONObject("avatar")?.let { text(it, "large") },
            banner = text(json, "bannerImage"),
            about = text(json, "about").orEmpty(),
            badge = text(json, "donatorBadge")?.takeIf { json.optInt("donatorTier") > 0 },
            isSelf = AniListSession.viewer?.id == id,
            isFollowing = json.optBoolean("isFollowing"),
            isFollower = json.optBoolean("isFollower"),
            siteUrl = text(json, "siteUrl"),
            anime = totals(statistics?.optJSONObject("anime"), "minutesWatched", MinutesPerDay),
            manga = totals(statistics?.optJSONObject("manga"), "chaptersRead", 1.0),
            activity = days(json.optJSONObject("stats")?.optJSONArray("activityHistory")),
            animeGenres = genres(statistics?.optJSONObject("anime")),
            mangaGenres = genres(statistics?.optJSONObject("manga")),
            favourites = UserFavourites(
                anime = cards(favourites?.optJSONObject("anime")),
                manga = cards(favourites?.optJSONObject("manga")),
                characters = people(favourites?.optJSONObject("characters"), ProfileKind.Character),
                staff = people(favourites?.optJSONObject("staff"), ProfileKind.Staff),
                studios = studios(favourites?.optJSONObject("studios")),
                more = FavouriteKind.entries.filter { favourites?.optJSONObject(it.path)?.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true }.toSet()
            )
        )
        pages.put(pageKey(name), built)
        return built
    }

    // The key of a page or of its tail, which changes with the list of the user, as the totals on their own page do
    private fun pageKey(text: String): Int = "${text.lowercase()} ${ListChanges.version.value}".hashCode()

    // The last things the person did, newest first, titles moved and texts cut to their beginning, kept as long as the page
    suspend fun tail(id: Int): UserTail {
        tails.get(pageKey("tail $id"))?.let { return it.value }
        val root = client.query(TailQuery, JSONObject().put("id", id).put("size", RecentShown), AniListSession.token)
        val items = root?.optJSONObject("activity")?.let { activities(it) }.orEmpty()
        val found = UserTail(items, total(root?.optJSONObject("followers")), total(root?.optJSONObject("following")))
        if (root != null) tails.put(pageKey("tail $id"), found)
        return found
    }

    // Reads the total of a block from its page info
    private fun total(block: JSONObject?): Int = block?.optJSONObject("pageInfo")?.optInt("total") ?: 0

    // A page of what the person did, the newest first, null when AniList could not be reached
    suspend fun activityPage(id: Int, page: Int, size: Int = PageSize): UserActivityPage? {
        val root = client.query(RecentQuery, JSONObject().put("id", id).put("page", page).put("size", size), AniListSession.token) ?: return null
        val block = root.optJSONObject("Page") ?: return null
        return UserActivityPage(activities(block), block.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true)
    }

    // The things the person did that a page of AniList has, said in the language of the app
    private fun activities(block: JSONObject): List<UserActivity> =
        AniShelvesParser.activities(block.optJSONArray("activities")).mapNotNull { activity ->
            val card = activity.card
            val line = if (card != null) mapper.line(activity) else AboutSnippet.of(activity.text?.let { Media.replace(it, " ") })
            UserActivity(
                activity.id,
                line ?: return@mapNotNull null,
                card?.let { mapper.card(it, details = mapper.size(it)) },
                activity.createdAt,
                activity.likeCount,
                activity.replyCount,
                activity.isLiked,
                activity.isSubscribed
            )
        }

    // A page of the favourites of one kind, 25 at a time, null when AniList could not be reached
    suspend fun favourites(name: String, kind: FavouriteKind, page: Int): FavouritesPage? {
        val fields = when (kind) {
            FavouriteKind.Anime, FavouriteKind.Manga -> UserCardFields
            FavouriteKind.Characters, FavouriteKind.Staff -> PersonFields
            FavouriteKind.Studios -> "id name"
        }
        val query = "query (\$name: String, \$page: Int) { User(name: \$name) { favourites { ${kind.path}(page: \$page, perPage: $PageSize) { pageInfo { hasNextPage } nodes { $fields } } } } }"
        val root = client.query(query, JSONObject().put("name", name).put("page", page), AniListSession.token) ?: return null
        val block = root.optJSONObject("User")?.optJSONObject("favourites")?.optJSONObject(kind.path) ?: return null
        return FavouritesPage(
            cards = if (kind == FavouriteKind.Anime || kind == FavouriteKind.Manga) cards(block) else emptyList(),
            people = when (kind) {
                FavouriteKind.Characters -> people(block, ProfileKind.Character)
                FavouriteKind.Staff -> people(block, ProfileKind.Staff)
                else -> emptyList()
            },
            studios = if (kind == FavouriteKind.Studios) studios(block) else emptyList(),
            hasMore = block.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true
        )
    }

    // Follows the person or stops following them, and says what it is now, null when AniList did not take it
    suspend fun toggleFollow(name: String, id: Int): Boolean? {
        val root = client.query(FollowQuery, JSONObject().put("id", id), AniListSession.token) ?: return null

        // What was kept of the page no longer says whether the user follows them, nor how many follow them
        pages.remove(pageKey(name))
        tails.remove(pageKey("tail $id"))
        return root.optJSONObject("ToggleFollow")?.optBoolean("isFollowing")
    }

    // The totals of a kind: the amount is the field that says how much of it was done, divided by the unit the page shows it in
    private fun totals(json: JSONObject?, amountKey: String, unit: Double) =
        UserTotals(json?.optInt("count") ?: 0, (json?.optDouble(amountKey, 0.0) ?: 0.0) / unit, json?.optDouble("meanScore", 0.0) ?: 0.0)

    // The genres in the language of the app, the name AniList gives when the app has no other for it
    private fun genres(json: JSONObject?): List<UserGenre> =
        objects(json?.optJSONArray("genres")).mapNotNull { genre -> text(genre, "genre")?.let { UserGenre(genreNames[it] ?: it, genre.optInt("count")) } }

    // Reads the studios of a favourites block
    private fun studios(block: JSONObject?): List<UserStudio> =
        nodes(block).mapNotNull { studio ->
            val studioId = studio.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
            UserStudio(text(studio, "name") ?: return@mapNotNull null, ProfileRef(ProfileKind.Studio, studioId))
        }

    // The days of the year the person was active, the date comes in seconds
    private fun days(list: JSONArray?): List<ActivityDay> =
        objects(list).map { day ->
            ActivityDay(Instant.ofEpochSecond(day.optLong("date")).atZone(ZoneOffset.UTC).toLocalDate(), day.optInt("amount"), day.optInt("level"))
        }

    // The titles of a kind of favourite, each once, with the kind of title and its year in the line under it
    private fun cards(block: JSONObject?): List<CardUi> =
        nodes(block).mapNotNull { AniShelvesParser.card(it) }.distinctBy { it.id }.map { card -> mapper.card(card, details = mapper.size(card)) }

    // Reads the people of a favourites block
    private fun people(block: JSONObject?, kind: ProfileKind): List<TitlePerson> =
        nodes(block).mapNotNull { person ->
            val id = person.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
            val name = person.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            TitlePerson(name, person.optJSONObject("image")?.let { text(it, "large") }, null, null, "https://anilist.co/${kind.path}/$id", ProfileRef(kind, id))
        }

    // Reads the nodes of a block
    private fun nodes(block: JSONObject?): List<JSONObject> = objects(block?.optJSONArray("nodes"))

    // Reads a JSON array as objects
    private fun objects(list: JSONArray?): List<JSONObject> = if (list == null) emptyList() else (0 until list.length()).map { list.getJSONObject(it) }

    // Reads a text, null when it is missing or empty
    private fun text(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() && it != "null" }

    // Constants of the repository
    private companion object {
        // The minutes of a day, which is what the days watched are counted from
        const val MinutesPerDay = 1_440.0

        // The pages that were opened, kept for five minutes, so going to a title or to a person and back does not ask again
        private const val PagesMillis = 5L * 60L * 1000L
        private const val MaxPages = 20
        private val pages = TimedCache<UserUi>(PagesMillis, MaxPages)
        private val tails = TimedCache<UserTail>(PagesMillis, MaxPages)

        // How many of each kind of favourite come with the page, the rest is in the page of its kind, and how many genres
        const val FavouritesShown = 10
        const val GenresShown = 8

        // How many of the last things the person did come, and the pictures and videos of a text, which are left out of its beginning
        const val RecentShown = 6
        val Media = Regex("(?:img|webm|youtube)\\d*%?\\([^)]*\\)|!\\[[^\\]]*]\\([^)]*\\)", RegexOption.IGNORE_CASE)

        // How many items a page has
        const val PageSize = 25

        // A title needs less here than on the shelves: the cover that is not the biggest, which arrives sooner, and no wide picture
        val UserCardFields = AniShelvesParser.CardFields.replace("bannerImage ", "").replace("coverImage { extraLarge large }", "coverImage { large }")

        // The fields of a person and the query of what they did lately
        const val PersonFields = "id name { userPreferred } image { large }"
        const val RecentQuery = "query (\$id: Int, \$page: Int, \$size: Int) { Page(page: \$page, perPage: \$size) { pageInfo { hasNextPage } activities(userId: \$id, sort: ID_DESC) { __typename " +
            "... on ListActivity { id createdAt likeCount replyCount isLiked isSubscribed status progress user { id name } media { ${AniShelvesParser.CardFields} } } " +
            "... on TextActivity { id createdAt likeCount replyCount isLiked isSubscribed text(asHtml: false) user { id name } } } } }"

        // What comes after the page: the last things they did, and how many follow them and are followed, from the totals
        val TailQuery = "query (\$id: Int!, \$size: Int) { activity: Page(page: 1, perPage: \$size) { activities(userId: \$id, sort: ID_DESC) { __typename " +
            "... on ListActivity { id createdAt likeCount replyCount isLiked isSubscribed status progress user { id name } media { ${AniShelvesParser.CardFields} } } " +
            "... on TextActivity { id createdAt likeCount replyCount isLiked isSubscribed text(asHtml: false) user { id name } } } } " +
            "followers: Page(perPage: 1) { pageInfo { total } followers(userId: \$id) { id } } " +
            "following: Page(perPage: 1) { pageInfo { total } following(userId: \$id) { id } } }"
        val UserQuery = "query (\$name: String) { User(name: \$name) { id name createdAt previousNames { name } about(asHtml: false) bannerImage avatar { large } " +
            "donatorTier donatorBadge isFollowing isFollower siteUrl stats { activityHistory { date amount level } } " +
            "statistics { anime { count minutesWatched meanScore genres(limit: $GenresShown, sort: COUNT_DESC) { genre count } } " +
            "manga { count chaptersRead meanScore genres(limit: $GenresShown, sort: COUNT_DESC) { genre count } } } " +
            "favourites { anime(perPage: $FavouritesShown) { pageInfo { hasNextPage } nodes { $UserCardFields } } " +
            "manga(perPage: $FavouritesShown) { pageInfo { hasNextPage } nodes { $UserCardFields } } " +
            "characters(perPage: $FavouritesShown) { pageInfo { hasNextPage } nodes { $PersonFields } } " +
            "staff(perPage: $FavouritesShown) { pageInfo { hasNextPage } nodes { $PersonFields } } " +
            "studios(perPage: $FavouritesShown) { pageInfo { hasNextPage } nodes { id name } } } } }"

        // The mutation that follows or unfollows a person
        const val FollowQuery = "mutation (\$id: Int) { ToggleFollow(userId: \$id) { id isFollowing } }"
    }
}
