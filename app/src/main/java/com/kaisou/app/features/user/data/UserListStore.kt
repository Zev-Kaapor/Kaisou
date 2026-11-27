package com.kaisou.app.features.user.data

// Android framework
import android.content.Context
// Dates
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Kotlin coroutines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.artwork.ArtQuery
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.rating.RatingQuery
import com.kaisou.app.features.settings.data.AdultContent
import com.kaisou.app.features.settings.data.AdultMode
import com.kaisou.app.features.title.data.ListChanges

// A title on the list of a person: picture, names, kind, progress, scores, state, times, year, genres, facts and what rates it
data class UserListEntry(
    val mediaId: Int,
    val title: String,
    val romaji: String,
    val cover: String?,
    val format: String?,
    val formatKey: String?,
    val source: String?,
    val airing: String?,
    val season: String?,
    val country: String?,
    val tags: List<String>,
    val total: Int?,
    val progress: Int,
    val score: Int,
    val status: String,
    val updatedAt: Long,
    val addedAt: Long,
    val startedOn: Int,
    val completedOn: Int,
    val releaseOn: Int,
    val popularity: Int,
    val year: Int?,
    val genres: List<String>,
    // The score of everyone from 0 to 100, and what the age rating needs: the MyAnimeList id, the adult mark and the kind
    val average: Int,
    val malId: Int?,
    val adult: Boolean,
    val manga: Boolean,
    val isMovie: Boolean
) {
    // What finds the age rating of the title, like the cards of the shelves do
    fun ratingQuery() = RatingQuery(mediaId, malId, adult, manga, ArtQuery(listOfNotNull(title, romaji.takeIf { it.isNotBlank() && it != title }), year, isMovie))

    // The day they finished it: the one they set, or else the last time they changed it
    fun finishedDate(): LocalDate {
        val changed = Instant.ofEpochSecond(updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        if (completedOn <= 0) return changed
        return runCatching { LocalDate.of(completedOn / 10000, completedOn / 100 % 100, completedOn % 100) }.getOrDefault(changed)
    }
}

// One list of a person: a state of AniList, by its key, or a list the person made, by its name, with its titles
class UserListSection(val key: String, val custom: Boolean, val entries: List<UserListEntry>)

// The lists of anime and manga of a person in one question, kept ten minutes and shared by the profile and the Library
object UserListStore {
    // How long and how many lists are kept, the format of a movie, and the fields asked for each list
    private const val MovieFormat = "MOVIE"
    private const val LifeMillis = 10L * 60L * 1000L
    private const val MaxLists = 8
    private const val Lists = "lists { name isCustomList status entries { status progress score(format: POINT_100) createdAt updatedAt startedAt { year month day } completedAt { year month day } media { id title { english romaji } " +
        "coverImage { large } format status source season countryOfOrigin tags { name } episodes chapters genres startDate { year month day } popularity averageScore idMal isAdult } } }"

    // The query of the anime and the manga lists
    private const val Query = "query (\$name: String) { anime: MediaListCollection(userName: \$name, type: ANIME, forceSingleCompletedList: true) { $Lists } " +
        "manga: MediaListCollection(userName: \$name, type: MANGA, forceSingleCompletedList: true) { $Lists } }"

    private val client = AniListClient()
    private val kept = TimedCache<List<UserListSection>>(LifeMillis, MaxLists)
    private val asking = HashMap<String, CompletableDeferred<Pair<List<UserListSection>, List<UserListSection>>?>>()
    private val lock = Mutex()
    private var seenVersion = ListChanges.version.value

    // The lists of the person, from what is kept when it is there. Null when AniList could not be reached or the lists are private
    suspend fun lists(context: Context, name: String, manga: Boolean): List<UserListSection>? = cached(context, name, manga)?.let(::visible)

    // Leaves out of the lists the titles the user chose not to see, or all but them
    private fun visible(sections: List<UserListSection>): List<UserListSection> =
        if (AdultContent.mode.value == AdultMode.Show) sections else sections.map { UserListSection(it.key, it.custom, it.entries.filter { entry -> AdultContent.allows(entry.adult) }) }

    // The lists as they were asked for, with every title
    private suspend fun cached(context: Context, name: String, manga: Boolean): List<UserListSection>? {
        val person = name.lowercase()
        val (mine, wait) = lock.withLock {
            // A change of the list of the user makes what was kept old
            if (ListChanges.version.value != seenVersion) {
                seenVersion = ListChanges.version.value
                kept.clear()
            }
            kept.get(key(person, manga))?.let { return it.value }
            asking[person]?.let { return@withLock false to it }
            true to CompletableDeferred<Pair<List<UserListSection>, List<UserListSection>>?>().also { asking[person] = it }
        }
        val found = if (mine) ask(context, name) else wait.await()
        if (mine) {
            lock.withLock {
                found?.let { (anime, mangaLists) ->
                    kept.put(key(person, false), anime)
                    kept.put(key(person, true), mangaLists)
                }
                asking.remove(person)
            }
            wait.complete(found)
        }
        return found?.let { if (manga) it.second else it.first }
    }

    // Makes the key of a person and a kind
    private fun key(person: String, manga: Boolean): Int = "$person $manga".hashCode()

    // The anime and the manga of the person, null when AniList could not be reached or the lists are private
    private suspend fun ask(context: Context, name: String): Pair<List<UserListSection>, List<UserListSection>>? {
        val root = client.query(Query, JSONObject().put("name", name), AniListSession.token, keep = false) ?: return null
        val resources = context.resources
        val formats = resources.getStringArray(R.array.format_keys).zip(resources.getStringArray(R.array.format_names)).toMap()
        val anime = root.optJSONObject("anime")?.let { sections(it, false, formats) } ?: return null
        val manga = root.optJSONObject("manga")?.let { sections(it, true, formats) } ?: return null
        return anime to manga
    }

    // Reads the lists of a collection
    private fun sections(collection: JSONObject, manga: Boolean, formats: Map<String, String>): List<UserListSection> =
        objects(collection.optJSONArray("lists")).mapNotNull { list ->
            val custom = list.optBoolean("isCustomList")
            val key = (if (custom) text(list, "name") else text(list, "status")) ?: return@mapNotNull null
            UserListSection(key, custom, objects(list.optJSONArray("entries")).mapNotNull { entry(it, manga, formats) })
        }

    // Reads one entry of a list, null when it has no title
    private fun entry(entry: JSONObject, manga: Boolean, formats: Map<String, String>): UserListEntry? {
        val media = entry.optJSONObject("media") ?: return null
        val id = media.optInt("id").takeIf { it > 0 } ?: return null
        val title = media.optJSONObject("title")
        val romaji = title?.let { text(it, "romaji") }.orEmpty()
        return UserListEntry(
            mediaId = id,
            title = title?.let { text(it, "english") } ?: romaji,
            romaji = romaji,
            cover = media.optJSONObject("coverImage")?.let { text(it, "large") },
            format = text(media, "format")?.let { formats[it] ?: it },
            formatKey = text(media, "format"),
            source = text(media, "source"),
            airing = text(media, "status"),
            season = text(media, "season"),
            country = text(media, "countryOfOrigin"),
            tags = objects(media.optJSONArray("tags")).mapNotNull { text(it, "name") },
            total = (if (manga) media.optInt("chapters") else media.optInt("episodes")).takeIf { it > 0 },
            progress = entry.optInt("progress"),
            score = entry.optInt("score"),
            status = text(entry, "status").orEmpty(),
            updatedAt = entry.optLong("updatedAt"),
            addedAt = entry.optLong("createdAt"),
            startedOn = day(entry.optJSONObject("startedAt")),
            completedOn = day(entry.optJSONObject("completedAt")),
            releaseOn = day(media.optJSONObject("startDate")),
            popularity = media.optInt("popularity"),
            year = media.optJSONObject("startDate")?.optInt("year")?.takeIf { it > 0 },
            genres = strings(media.optJSONArray("genres")),
            average = media.optInt("averageScore"),
            malId = media.optInt("idMal").takeIf { it > 0 },
            adult = media.optBoolean("isAdult"),
            manga = manga,
            isMovie = text(media, "format") == MovieFormat
        )
    }

    // Reads a day as a number of the year, the month and the day, 0 when there is no year
    private fun day(json: JSONObject?): Int =
        json?.let { it.optInt("year").takeIf { year -> year > 0 }?.let { year -> year * 10000 + it.optInt("month").coerceAtLeast(1) * 100 + it.optInt("day").coerceAtLeast(1) } } ?: 0

    // Reads a JSON array as objects
    private fun objects(list: JSONArray?): List<JSONObject> = if (list == null) emptyList() else (0 until list.length()).mapNotNull { list.optJSONObject(it) }

    // Reads a JSON array as texts
    private fun strings(list: JSONArray?): List<String> = if (list == null) emptyList() else (0 until list.length()).mapNotNull { list.optString(it).takeIf { s -> s.isNotBlank() } }

    // Reads a text, null when it is missing or empty
    private fun text(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() && it != "null" }
}
