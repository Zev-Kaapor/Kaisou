package com.kaisou.app.features.title.data

// Android framework
import android.content.Context
import android.net.Uri
// UI styling and utilities
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.toColorInt
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java time and utilities
import java.net.URLEncoder
import java.text.Collator
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.random.Random
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniDescription
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.anilist.AniShelvesParser
import com.kaisou.app.core.anilist.SeasonNumber
import com.kaisou.app.core.anilist.StaffRoles
import com.kaisou.app.core.artwork.ArtQuery
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.designsystem.theme.Background
import com.kaisou.app.core.designsystem.theme.ContainerHigh
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.language.DubLanguage
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.data.HomeMapper
import com.kaisou.app.features.profile.data.ProfileKind
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.core.anilist.AboutSnippet
import com.kaisou.app.core.net.HttpText
import com.kaisou.app.core.rating.AgeGroup
import com.kaisou.app.core.rating.AgeTag
import com.kaisou.app.core.rating.MangaAgeEstimate

// Asks AniList for the title of a page and turns the answer into what the screen shows. Prototype: the top of the page and the details
class TitleRepository(context: Context) {
    private val client = AniListClient()
    private val resources = context.resources
    private val formatNames by lazy { names(R.array.format_keys, R.array.format_names) }
    private val genreNames by lazy { names(R.array.genre_keys, R.array.genre_names) }
    private val sourceNames by lazy { names(R.array.title_source_keys, R.array.title_source_names) }
    private val seasonNames by lazy { names(R.array.title_season_keys, R.array.title_season_names) }
    private val roleNames by lazy { names(R.array.title_role_keys, R.array.title_role_names) }
    private val staffRoles by lazy { StaffRoles(resources.getStringArray(R.array.title_staff_keys), resources.getStringArray(R.array.title_staff_names)) }
    private val relationNames by lazy { names(R.array.title_relation_keys, R.array.title_relation_names) }
    private val mapper = HomeMapper(resources)

    // The title of the page, null when AniList could not be reached or does not know it
    suspend fun header(id: Int): TitleUi? {
        val data = askAll(HeaderQuery, id) ?: return null
        val title = data.optJSONObject("Media")?.let(::parse) ?: return null
        return title.copy(friends = friends(data.optJSONObject("friends")?.optJSONArray("mediaList"), title.manga))
    }

    // Everything that comes after the top, in one question to AniList and not three: the details, what other people recommend and the
    // characters and the staff. Each page asking for a handful of questions was what made AniList ask the app to rest, so what it says is
    // kept on the phone for a while. It is the same for everyone, the heart and the list are in the top, which is never kept. Null when
    // AniList could not be reached
    suspend fun extras(id: Int): TitleExtras? {
        val variables = JSONObject().put("id", id)
        val raw = kept(ExtrasSpace, id.toString()) { client.query(ExtrasQuery, variables, AniListSession.token)?.toString() } ?: return null
        val data = JSONObject(raw)
        val details = data.optJSONObject("details") ?: return null
        val parsed = parseDetails(details)
        val people = cast(data.optJSONObject("cast"))
        // The ones that made the title are in the data too, by what they did, each one a link
        val makers = people.creators.groupBy { it.role }.map { (role, list) -> TitleDatum(role.orEmpty(), list.map { it.name }, list.map { it.ref }) }
        return TitleExtras(
            details = parsed.copy(tags = inTheLanguage(parsed.tags), data = afterStudios(parsed.data, makers)),
            recommendations = recommendations(data.optJSONObject("recommended")),
            people = people
        )
    }

    // The friends that have the title on a list, with where it is, how far they are and the score they gave, the user left out
    private fun friends(list: JSONArray?, manga: Boolean): List<TitleFriend> =
        objects(list).mapNotNull { entry ->
            val user = entry.optJSONObject("user") ?: return@mapNotNull null
            val name = text(user, "name") ?: return@mapNotNull null
            TitleFriend(
                name = name,
                avatar = user.optJSONObject("avatar")?.let { text(it, "large") },
                status = listNames(manga)[text(entry, "status")].orEmpty(),
                progress = number(entry, "progress"),
                score = entry.optDouble("score", 0.0).takeIf { it > 0 }?.toFloat(),
                url = "https://anilist.co/user/$name"
            )
        }.filterNot { it.name.equals(AniListSession.viewer?.name, ignoreCase = true) }

    // The names of the tags in the language of the app, in the alphabetical order of that language. A name that is not translated stays as
    // AniList has it
    private suspend fun inTheLanguage(tags: List<TitleTag>): List<TitleTag> {
        if (!AppLanguage.needsTranslation || tags.isEmpty()) return tags
        val names = GoogleTranslator.translateLines(tags.map { it.name }, AppLanguage.translationCode)
        val collator = Collator.getInstance(Locale.getDefault())
        return tags.zip(names) { tag, name -> tag.copy(name = name) }.sortedWith(compareBy(collator) { it.name })
    }

    // The ones that made the title put right after the studios and the producers, or after the numbers when it has none, as AniList does
    private fun afterStudios(data: List<TitleDatum>, makers: List<TitleDatum>): List<TitleDatum> {
        val anchors = listOf(R.string.title_data_producers, R.string.title_data_studios, R.string.title_data_favourites).map(resources::getString)
        val at = anchors.firstNotNullOfOrNull { label -> data.indexOfFirst { it.label == label }.takeIf { it >= 0 } } ?: (data.size - 1)
        return data.take(at + 1) + makers + data.drop(at + 1)
    }

    // What was kept on the phone for a title when it is still good, or what AniList says, which is then kept. Nothing kept when it failed
    private suspend fun kept(space: String, key: String, load: suspend () -> String?): String? {
        withContext(Dispatchers.IO) { AppStore.get(space, key, KeptMillis) }?.let { return it }
        val fresh = load() ?: return null
        withContext(Dispatchers.IO) { AppStore.put(space, key, fresh) }
        return fresh
    }

    // What other people recommend after this title, as cards, the ones that most people agree on first
    private fun recommendations(media: JSONObject?): List<CardUi> =
        objects(media?.optJSONObject("recommendations")?.optJSONArray("nodes"))
            .filter { it.optInt("rating") > 0 }
            .mapNotNull { AniShelvesParser.card(it.optJSONObject("mediaRecommendation")) }
            .distinctBy { it.id }
            .map { mapper.card(it, details = mapper.size(it)) }

    // The characters with the person that voices them and the staff
    private fun cast(media: JSONObject?): TitlePeople =
        TitlePeople(
            characters = people(media?.optJSONObject("characters"), CharacterPage, DubLanguage.ofCountry(media?.let { text(it, "countryOfOrigin") }), ::voicesOf),
            staff = people(media?.optJSONObject("staff"), StaffPage) { emptyList() },
            creators = creators(media?.optJSONObject("staff")),
            moreCharacters = media?.optJSONObject("characters")?.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true,
            moreStaff = media?.optJSONObject("staff")?.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true
        )

    // The people of the staff with a part that makes the title: who wrote it and drew it, who directed it, who made its music. Each once, with
    // the first parts that AniList puts before the others
    private fun creators(block: JSONObject?): List<TitlePerson> =
        objects(block?.optJSONArray("edges")).mapNotNull { edge ->
            val raw = text(edge, "role") ?: return@mapNotNull null
            if (!CreatorRole.containsMatchIn(raw)) return@mapNotNull null
            val node = edge.optJSONObject("node") ?: return@mapNotNull null
            val id = node.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
            val name = node.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            TitlePerson(name, node.optJSONObject("image")?.let { text(it, "large") }, roleNames[raw] ?: staffRoles.translate(raw), null, "https://anilist.co/staff/$id", ProfileRef(ProfileKind.Staff, id))
        }.distinctBy { it.url }.take(MaxCreators)

    // The voices of a character in all the languages AniList has, who voices it in each, as AniList lists them
    private fun voicesOf(edge: JSONObject): List<TitleVoice> =
        objects(edge.optJSONArray("voiceActors")).mapNotNull { actor ->
            val name = actor.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            TitleVoice(name, text(actor, "languageV2") ?: return@mapNotNull null)
        }.distinct()

    // The voice that shows under a character on the page of the title: the one in the original language of the title, and when there is none
    // the first that came
    private fun shownVoice(voices: List<TitleVoice>, original: DubLanguage): TitleVoice? =
        voices.firstOrNull { it.language.equals(original.englishName, ignoreCase = true) } ?: voices.firstOrNull()

    private fun people(block: JSONObject?, kind: String, original: DubLanguage = DubLanguage.Japanese, voicesOf: (JSONObject) -> List<TitleVoice>): List<TitlePerson> {
        val edges = block?.optJSONArray("edges") ?: return emptyList()
        return (0 until edges.length()).mapNotNull { index ->
            val edge = edges.getJSONObject(index)
            val node = edge.optJSONObject("node") ?: return@mapNotNull null
            val name = node.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            TitlePerson(
                name = name,
                image = node.optJSONObject("image")?.let { text(it, "large") },
                // A character has a role in the title, a person of the staff has what they did
                role = text(edge, "role")?.let { roleNames[it] ?: staffRoles.translate(it) },
                voice = shownVoice(voicesOf(edge), original)?.name,
                about = AboutSnippet.of(text(node, "description")),
                url = "https://anilist.co/$kind/${node.getInt("id")}",
                ref = ProfileRef(ProfileKind.of(kind), node.getInt("id"))
            )
        }.distinctBy { it.url }
    }

    // The beginnings of the people of a page in the language of the app, all in one go
    private suspend fun withAboutTranslated(people: List<TitlePerson>): List<TitlePerson> {
        val withAbout = people.filter { it.about != null }
        if (withAbout.isEmpty()) return people
        val translated = AboutSnippet.translated(withAbout.map { it.about.orEmpty() })
        val byUrl = withAbout.map { it.url }.zip(translated).toMap()
        return people.map { person -> byUrl[person.url]?.let { person.copy(about = it) } ?: person }
    }

    // A page of all the characters or all the staff of a title, 25 at a time, null when AniList could not be reached
    suspend fun castPage(id: Int, staff: Boolean, page: Int): CastPage? {
        val variables = JSONObject().put("id", id).put("page", page)
        val media = client.query(if (staff) CastStaffQuery else CastCharactersQuery, variables, AniListSession.token)?.optJSONObject("Media") ?: return null
        val block = media.optJSONObject(if (staff) "staff" else "characters")
        val people = if (staff) {
            people(block, StaffPage) { emptyList() }
        } else {
            people(block, CharacterPage, DubLanguage.ofCountry(text(media, "countryOfOrigin")), ::voicesOf)
        }
        return CastPage(withAboutTranslated(people), block?.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true)
    }

    // The reviews, how the scores and the lists are spread and the threads of the forum. Null when
    // AniList could not be reached
    suspend fun community(id: Int, manga: Boolean): TitleCommunity? {
        val variables = JSONObject().put("id", id).put("fresh", -Random.nextInt(1, Int.MAX_VALUE))
        val data = client.query(CommunityQuery, variables, AniListSession.token) ?: return null
        val media = data.optJSONObject("Media")
        val threads = data.optJSONObject("threads")?.optJSONArray("threads")
        val reviews = media?.optJSONObject("reviews")?.optJSONArray("nodes")
        val stats = media?.optJSONObject("stats")
        return TitleCommunity(
            reviews = objects(reviews).take(MaxReviews).mapNotNull(::reviewOf),
            scores = objects(stats?.optJSONArray("scoreDistribution")).map { TitleScoreBar(it.optInt("score"), it.optInt("amount")) }.sortedBy { it.score },
            statuses = objects(stats?.optJSONArray("statusDistribution")).mapNotNull { share ->
                listNames(manga)[text(share, "status")]?.let { TitleStatusShare(it, share.optInt("amount")) }
            },
            activity = objects(data.optJSONObject("activity")?.optJSONArray("activities")).mapNotNull(::activityOf),
            popularity = media?.optInt("popularity") ?: 0,
            favourites = media?.optInt("favourites") ?: 0,
            trends = objects(media?.optJSONObject("trends")?.optJSONArray("nodes")).map { TitleTrend(it.optLong("date"), it.optInt("trending")) }.sortedBy { it.day },
            threads = objects(threads).mapNotNull { thread ->
                TitleThread(
                    title = text(thread, "title") ?: return@mapNotNull null,
                    replies = thread.optInt("replyCount"),
                    author = thread.optJSONObject("user")?.let { text(it, "name") }.orEmpty(),
                    url = "https://anilist.co/forum/thread/${thread.getInt("id")}"
                )
            }
        )
    }

    // What a person did to the title as AniList gives it, said in the language of the app, null when it has no author
    private fun activityOf(activity: JSONObject): TitleActivity? {
        val id = activity.optInt("id").takeIf { it > 0 } ?: return null
        val user = activity.optJSONObject("user") ?: return null
        val name = text(user, "name") ?: return null
        val status = text(activity, "status").orEmpty().lowercase()
        val progress = text(activity, "progress").orEmpty()
        val range = " - " in progress

        fun of(single: Int, several: Int) = resources.getString(if (range) several else single, progress)
        val said = when {
            status.startsWith("rewatched") -> of(R.string.activity_rewatched, R.string.activity_rewatched_range)
            status.startsWith("watched") -> of(R.string.activity_watched, R.string.activity_watched_range)
            status.startsWith("reread") -> of(R.string.activity_reread, R.string.activity_reread_range)
            status.startsWith("read") -> of(R.string.activity_read, R.string.activity_read_range)
            status.startsWith("plans to watch") -> resources.getString(R.string.activity_plans_watch)
            status.startsWith("plans to read") -> resources.getString(R.string.activity_plans_read)
            status.startsWith("completed") -> resources.getString(R.string.activity_completed)
            status.startsWith("paused") -> resources.getString(R.string.activity_paused)
            status.startsWith("dropped") -> resources.getString(R.string.activity_dropped)
            else -> return null
        }
        return TitleActivity(id, name, user.optJSONObject("avatar")?.let { text(it, "large") }, said, activity.optLong("createdAt"))
    }

    // A review as AniList gives it, null when it has no author
    private fun reviewOf(review: JSONObject): TitleReview? {
        val user = review.optJSONObject("user") ?: return null
        return TitleReview(
            id = review.getInt("id"),
            author = text(user, "name") ?: return null,
            avatar = user.optJSONObject("avatar")?.let { text(it, "large") },
            score = number(review, "score"),
            summary = text(review, "summary").orEmpty(),
            helpful = review.optInt("rating"),
            url = "https://anilist.co/review/${review.getInt("id")}",
            votes = review.optInt("ratingAmount"),
            createdAt = review.optLong("createdAt")
        )
    }

    // A page of the reviews of a title, the most useful first. Null when AniList could not be reached
    suspend fun reviewsPage(id: Int, page: Int): ReviewsPage? {
        val block = client.query(ReviewsQuery, JSONObject().put("id", id).put("page", page), AniListSession.token)?.optJSONObject("Page") ?: return null
        return ReviewsPage(objects(block.optJSONArray("reviews")).mapNotNull(::reviewOf), block.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true)
    }

    // A review with its whole text. Null when AniList could not be reached or does not have it
    suspend fun review(id: Int): ReviewBody? {
        val media = client.query(ReviewQuery, JSONObject().put("id", id), AniListSession.token)?.optJSONObject("Review") ?: return null
        val review = reviewOf(media) ?: return null
        return ReviewBody(review, text(media, "body").orEmpty(), voteOf(text(media, "userRating")))
    }

    // Votes on a review: the thumb up, the thumb down, or takes the vote back. This changes the account of the user, so it is only called when
    // they touch a thumb. Null when AniList did not take it
    suspend fun rateReview(id: Int, vote: ReviewVote): ReviewTally? {
        val rating = when (vote) {
            ReviewVote.Up -> "UP_VOTE"
            ReviewVote.Down -> "DOWN_VOTE"
            ReviewVote.None -> "NO_VOTE"
        }
        val result = client.query(RateReviewQuery, JSONObject().put("id", id).put("rating", rating), AniListSession.token)?.optJSONObject("RateReview") ?: return null
        return ReviewTally(result.optInt("rating"), result.optInt("ratingAmount"), voteOf(text(result, "userRating")))
    }

    private fun voteOf(rating: String?): ReviewVote = when (rating) {
        "UP_VOTE" -> ReviewVote.Up
        "DOWN_VOTE" -> ReviewVote.Down
        else -> ReviewVote.None
    }

    // What the user has done with the title, as AniList names the lists in the language of the app: reading for a manga, watching for
    // an anime
    private fun listNames(manga: Boolean): Map<String, String> =
        if (manga) names(R.array.title_list_keys, R.array.title_list_names_manga) else names(R.array.title_list_keys, R.array.title_list_names_anime)

    private fun objects(list: JSONArray?): List<JSONObject> = if (list == null) emptyList() else (0 until list.length()).map { list.getJSONObject(it) }

    // The whole series of an anime or of a manga in the order to take it, with a card for each title. What is tied to this title is known
    // from its details, so the first question already goes two steps from it. What was found is kept on the phone, a series does not change.
    // Null when AniList could not be reached
    suspend fun watchOrder(id: Int, manga: Boolean, relations: List<TitleRelation>, malId: Int?): TitleWatchOrder? {
        val ordered = (if (!manga && malId != null) chiakiOrder(malId) else null) ?: run {
            val seeds = relations.filter { it.type in SeriesTies }.map { it.card.anilistId }
            val raw = keptSeries(id, seeds, manga) ?: return null
            inOrder(objects(JSONArray(raw)))
        }
        val separator = resources.getString(R.string.home_card_separator)
        val cards = ordered.mapNotNull { item ->
            val card = AniShelvesParser.card(item) ?: return@mapNotNull null
            val meta = listOfNotNull(card.format?.let { formatNames[it] }, card.year?.toString()).joinToString(separator)
            mapper.card(card, details = if (card.id == id) resources.getString(R.string.title_watch_current) else meta)
        }
        return TitleWatchOrder(cards, cards.indexOfFirst { it.anilistId == id }.coerceAtLeast(0))
    }

    // The titles of an anime in the order chiaki.site gives, as AniList has them. The site is asked by the id on MyAnimeList and answers with a
    // page that has a row for each title, and the titles come from AniList by those ids, so each has its card. What was found is kept on the
    // phone, for each title of the order, and the ones AniList does not know are left out. Null when the site has nothing for the title or
    // could not be reached, and then the ties on AniList are followed
    private suspend fun chiakiOrder(malId: Int): List<JSONObject>? {
        withContext(Dispatchers.IO) { AppStore.get(OrderSpace, malId.toString(), OrderMillis) }?.let { own ->
            val text = if (own.startsWith(SeriesPointer)) withContext(Dispatchers.IO) { AppStore.get(OrderSpace, own.removePrefix(SeriesPointer), OrderMillis) } else own
            if (text != null && !text.startsWith(SeriesPointer)) return objects(JSONArray(text))
        }
        val page = withContext(Dispatchers.IO) {
            try {
                HttpText.get(ChiakiOrderUrl + malId, headers = ChiakiHeaders)
            } catch (_: Exception) {
                null
            }
        } ?: return null
        val ids = ChiakiRow.findAll(page).map { it.groupValues[1].toInt() }.distinct().toList()
        if (ids.isEmpty()) return null
        val found = HashMap<Int, JSONObject>()
        for (batch in ids.chunked(SeriesBatch)) {
            val variables = JSONObject().put("ids", JSONArray(batch))
            val media = client.query(MalCardsQuery, variables, AniListSession.token)?.optJSONObject("Page")?.optJSONArray("media")
            if (media == null) {
                if (found.isEmpty()) return null
                break
            }
            objects(media).forEach { found[it.optInt("idMal")] = it }
        }
        val ordered = ids.mapNotNull { found[it] }
        if (ordered.isEmpty()) return null
        withContext(Dispatchers.IO) {
            AppStore.put(OrderSpace, malId.toString(), JSONArray(ordered).toString())
            ordered.forEach { item ->
                val member = item.optInt("idMal")
                if (member > 0 && member != malId) AppStore.put(OrderSpace, member.toString(), "$SeriesPointer$malId")
            }
        }
        return ordered
    }

    // The series as it was kept, or asked for. It is the same from each of its titles, so the one that was asked keeps all of it and the
    // others keep only the id of that one, and going from one title to the next asks nothing, nor fills the phone with the same list
    private suspend fun keptSeries(id: Int, seeds: List<Int>, manga: Boolean): String? {
        val own = withContext(Dispatchers.IO) { AppStore.get(SeriesSpace, id.toString(), KeptMillis) }
        if (own != null && !own.startsWith(SeriesPointer)) return own
        if (own != null) {
            val target = withContext(Dispatchers.IO) { AppStore.get(SeriesSpace, own.removePrefix(SeriesPointer), KeptMillis) }
            if (target != null && !target.startsWith(SeriesPointer)) return target
        }
        val found = series(id, seeds, manga) ?: return null
        val text = found.toString()
        withContext(Dispatchers.IO) {
            AppStore.put(SeriesSpace, id.toString(), text)
            objects(found).forEach { item ->
                val member = item.getInt("id")
                if (member != id) AppStore.put(SeriesSpace, member.toString(), "$SeriesPointer$id")
            }
        }
        return text
    }

    // The day a title started as a number that can be sorted, the ones with no day last
    private fun startKey(item: JSONObject): Int {
        val start = item.optJSONObject("startDate")
        val year = start?.let { number(it, "year") } ?: return Int.MAX_VALUE
        return year * 10_000 + (number(start, "month") ?: 0) * 100 + (number(start, "day") ?: 0)
    }

    // The titles of a series in the order to take them. What AniList says comes before another comes first, even when it came out later, as
    // a prequel that was drawn after the story it tells the start of, and the ones with nothing tying them in that way go by the day they started
    private fun inOrder(items: List<JSONObject>): List<JSONObject> {
        val ids = items.map { it.getInt("id") }.toSet()
        val before = mutableMapOf<Int, MutableSet<Int>>()
        items.forEach { item ->
            val id = item.getInt("id")
            objects(item.optJSONObject("relations")?.optJSONArray("edges")).forEach { edge ->
                val other = edge.optJSONObject("node")?.optInt("id") ?: return@forEach
                if (other !in ids) return@forEach
                when (text(edge, "relationType")) {
                    "PREQUEL" -> before.getOrPut(id) { mutableSetOf() }.add(other)
                    "SEQUEL" -> before.getOrPut(other) { mutableSetOf() }.add(id)
                }
            }
        }
        val remaining = items.sortedBy(::startKey).toMutableList()
        val result = mutableListOf<JSONObject>()
        while (remaining.isNotEmpty()) {
            // The first by day that has nothing left before it, and when they wait for each other in a loop, the first by day
            val waiting = remaining.map { it.getInt("id") }.toSet()
            val next = remaining.firstOrNull { item -> before[item.getInt("id")].orEmpty().none { it in waiting } } ?: remaining.first()
            result += next
            remaining -= next
        }
        return result
    }

    // The titles of the series, as AniList has them. It starts from this title and the ones tied to it and follows what comes before and
    // after and the stories apart, a few questions of many titles each, until there is nothing new or it is big enough. The first question
    // failing is a failure, a later one only ends the search
    private suspend fun series(id: Int, seeds: List<Int>, manga: Boolean): JSONArray? {
        val found = LinkedHashMap<Int, JSONObject>()
        var next = (listOf(id) + seeds).distinct()
        var hops = 0
        while (next.isNotEmpty() && found.size < MaxSeries && hops < MaxHops) {
            val variables = JSONObject().put("ids", JSONArray(next.take(SeriesBatch))).put("type", if (manga) MangaType else AnimeType)
            val media = client.query(WatchOrderQuery, variables, AniListSession.token)?.optJSONObject("Page")?.optJSONArray("media")
            if (media == null) {
                if (found.isEmpty()) return null
                break
            }
            val tied = mutableListOf<Int>()
            objects(media).forEach { item ->
                found[item.getInt("id")] = item
                objects(item.optJSONObject("relations")?.optJSONArray("edges")).forEach { edge ->
                    val node = edge.optJSONObject("node") ?: return@forEach
                    if (text(edge, "relationType") in SeriesTies && text(node, "type") == (if (manga) MangaType else AnimeType)) tied += node.getInt("id")
                }
            }
            next = tied.distinct().filter { it !in found }
            hops++
        }
        return JSONArray(found.values.toList())
    }

    // The episodes of an anime, with the ones that ani.zip does not have yet as numbers only, up to the last that aired. Null when ani.zip
    // could not be reached
    suspend fun episodes(title: TitleUi): EpisodeSet? {
        val found = EpisodeClient.episodes(title.anilistId, AppLanguage.code, AppLanguage.region) ?: return null
        val aired = title.episodes ?: title.nextAiring?.let { it.episode - 1 } ?: found.episodes.maxOfOrNull { it.number } ?: 0
        val known = found.episodes.associateBy { it.number }
        return EpisodeSet(
            episodes = (1..aired.coerceAtMost(MaxEpisodes)).map { known[it] ?: TitleEpisode(it, null, null, null, null) },
            tmdbId = found.tmdbId,
            imdbId = found.imdbId,
            malId = found.malId,
            tvdbId = found.tvdbId
        )
    }

    // What people score each episode, from 0 to 10 by the number of the episode: IMDb first, then MyAnimeList, then TVmaze, then TMDB. They are given
    // as each source answers
    suspend fun episodeRatings(found: EpisodeSet, onScores: suspend (Map<Int, Float>) -> Unit) = EpisodeRatings.rate(found, AppLanguage.code, AppLanguage.region, onScores)

    // The same episodes with their names and what they tell in the language of the app
    suspend fun translatedEpisodes(found: EpisodeSet): List<TitleEpisode> = EpisodeTranslator.translate(found, AppLanguage.code, AppLanguage.region)

    // AniList keeps its answer to a question for about a minute, for the same user, and gives it again to the same question, so the heart
    // and the list would show what they were a moment ago. A comment or a name does not make the question another, a value does: every
    // title passes a filter below any number, and the number is different each time, so each question is one that AniList has not seen
    // and the answer is always a new one
    private suspend fun askAll(query: String, id: Int): JSONObject? {
        val variables = JSONObject().put("id", id).put("fresh", -Random.nextInt(1, Int.MAX_VALUE))
        return client.query(query, variables, AniListSession.token)
    }

    private fun parse(media: JSONObject): TitleUi {
        val titles = media.getJSONObject("title")
        val english = text(titles, "english")
        val romaji = text(titles, "romaji")
        val names = listOfNotNull(english, romaji).distinct()
        val format = text(media, "format")
        val manga = text(media, "type") == MangaType
        val entry = media.optJSONObject("mediaListEntry")?.let(::listEntry)
        val listStatus = entry?.status
        val cover = media.optJSONObject("coverImage")
        val start = media.optJSONObject("startDate")
        val score = number(media, "averageScore") ?: number(media, "meanScore")
        val next = media.optJSONObject("nextAiringEpisode")?.takeUnless { it.isNull("airingAt") }
            ?.let { NextAiring(it.optInt("episode"), it.getLong("airingAt")) }
        val season = text(media, "season")?.let { seasonNames[it] }
        val seasonYear = number(media, "seasonYear")
        return TitleUi(
            anilistId = media.getInt("id"),
            manga = manga,
            malId = number(media, "idMal"),
            adult = media.optBoolean("isAdult"),
            seasonMarker = SeasonNumber.marker(names),
            partMarker = SeasonNumber.part(names),
            isContinuation = SeasonNumber.isContinuation(names),
            hasPrequel = media.optJSONObject("relations")?.optJSONArray("edges")?.let { edges ->
                (0 until edges.length()).any { edges.getJSONObject(it).optString("relationType") == PrequelRelation }
            } ?: false,
            startDate = start?.let(::localDate),
            title = english ?: romaji.orEmpty(),
            query = ArtQuery(names, start?.let { number(it, "year") }, format == MovieFormat),
            format = formatNames[format].orEmpty(),
            // The first genres, in the language of the app, the ones it does not know are left out
            genres = media.optJSONArray("genres")?.let { list -> (0 until list.length()).mapNotNull { genreNames[list.getString(it)] } }.orEmpty()
                .take(MaxGenres).joinToString(", "),
            year = start?.let { number(it, "year") },
            synopsis = AniDescription.markdown(text(media, "description")),
            releaseDay = next?.let { Instant.ofEpochSecond(it.airingAt).atZone(ZoneId.systemDefault()).dayOfWeek },
            score = score?.let { it / ScoreScale },
            listStatus = listStatus,
            nextUnit = entry?.nextUnit ?: 1,
            entry = entry,
            isFavourite = media.optBoolean("isFavourite"),
            releaseState = ReleaseState.of(text(media, "status")),
            nextAiring = next,
            episodes = number(media, "episodes"),
            chapters = number(media, "chapters"),
            volumes = number(media, "volumes"),
            duration = number(media, "duration"),
            seasonText = if (season != null && seasonYear != null) resources.getString(R.string.title_season_year, season, seasonYear) else null,
            studio = studios(media).firstOrNull { it.main && it.animation }?.name,
            coverUrl = cover?.let { text(it, "extraLarge") },
            bannerUrl = text(media, "bannerImage"),
            artColors = colors(cover?.let { text(it, "color") }),
            siteUrl = text(media, "siteUrl")
        )
    }

    private class Studio(val id: Int, val name: String, val main: Boolean, val animation: Boolean)

    private fun studioRef(studio: Studio): ProfileRef? = studio.id.takeIf { it > 0 }?.let { ProfileRef(ProfileKind.Studio, it) }

    private fun studios(media: JSONObject): List<Studio> {
        val edges = media.optJSONObject("studios")?.optJSONArray("edges") ?: return emptyList()
        return (0 until edges.length()).mapNotNull { index ->
            val edge = edges.getJSONObject(index)
            val node = edge.optJSONObject("node") ?: return@mapNotNull null
            text(node, "name")?.let { Studio(node.optInt("id"), it, edge.optBoolean("isMain"), node.optBoolean("isAnimationStudio")) }
        }
    }

    // The age a manga is for, from the tags and the genres in English as AniList gives them, before the names are put in the language of the app
    private fun mangaAge(media: JSONObject): AgeGroup? {
        val tags = objects(media.optJSONArray("tags")).mapNotNull { tag ->
            text(tag, "name")?.let { AgeTag(it, tag.optInt("rank"), tag.optBoolean("isMediaSpoiler"), tag.optBoolean("isAdult")) }
        }
        return MangaAgeEstimate.of(tags, strings(media.optJSONArray("genres")), media.optBoolean("isAdult"))
    }

    // The data of the title, the places it has among the best, the tags, the trailer and the links to other sites
    private fun parseDetails(media: JSONObject): TitleDetails {
        val numbers = NumberFormat.getIntegerInstance(Locale.getDefault())
        val manga = text(media, "type") == MangaType
        val titles = media.optJSONObject("title")
        val studios = studios(media)
        val data = buildList {
            fun addAll(label: Int, values: List<String>, refs: List<ProfileRef?> = emptyList(), urls: List<String?> = emptyList(), help: DatumHelp? = null) {
                if (values.isNotEmpty()) add(TitleDatum(resources.getString(label), values, refs, urls, help))
            }
            fun add(label: Int, value: String?, url: String? = null, help: DatumHelp? = null) =
                addAll(label, listOfNotNull(value?.takeIf { it.isNotBlank() }), urls = listOf(url), help = help)
            // In the order of the page of AniList: what it is, how long, when, how it is rated and liked, who made it, where it came from and
            // its names
            add(R.string.title_data_format, text(media, "format")?.let { formatNames[it] })
            // The country it comes from, right under the format, since a manga from Korea or from China is told by it
            add(R.string.title_data_origin, text(media, "countryOfOrigin")?.let { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.getDefault()).takeIf { name -> name.isNotBlank() } })
            if (manga) {
                add(R.string.title_data_chapters, number(media, "chapters")?.let(numbers::format))
                add(R.string.title_data_volumes, number(media, "volumes")?.let(numbers::format))
            } else {
                add(R.string.title_data_episodes, number(media, "episodes")?.let(numbers::format))
                add(R.string.title_data_duration, number(media, "duration")?.let { resources.getString(R.string.title_duration_minutes, it) })
            }
            add(R.string.title_data_status, ReleaseState.of(text(media, "status"))?.let { resources.getString(it.label) })
            add(R.string.title_data_start, media.optJSONObject("startDate")?.let(::dateText))
            add(R.string.title_data_end, media.optJSONObject("endDate")?.let(::dateText))
            if (!manga) {
                val season = text(media, "season")?.let { seasonNames[it] }
                val year = number(media, "seasonYear")
                // The season is a link to the search of AniList for the titles of that season
                val seasonName = text(media, "season")
                val seasonUrl = if (seasonName != null && year != null) "$SearchUrl/anime?year=$year%25&season=$seasonName" else null
                add(R.string.title_data_season, if (season != null && year != null) resources.getString(R.string.title_season_year, season, year) else null, seasonUrl)
            }
            // The weighted score has a hint that is a question mark, and the page of AniList that explains it
            add(
                R.string.title_data_weighted,
                number(media, "averageScore")?.let { resources.getString(R.string.title_data_percent, it) },
                help = DatumHelp(resources.getString(R.string.title_weighted_hint), WeightedScoreUrl)
            )
            add(R.string.title_data_mean, number(media, "meanScore")?.let { resources.getString(R.string.title_data_percent, it) })
            add(R.string.title_data_popularity, number(media, "popularity")?.let(numbers::format))
            add(R.string.title_data_favourites, number(media, "favourites")?.let(numbers::format))
            // The studios and the producers are links to their pages
            val animation = studios.filter { it.animation }
            val others = studios.filterNot { it.animation }
            addAll(R.string.title_data_studios, animation.map { it.name }, animation.map(::studioRef))
            addAll(R.string.title_data_producers, others.map { it.name }, others.map(::studioRef))
            add(R.string.title_data_source, text(media, "source")?.let { sourceNames[it] })
            // The hashtags search for them on X, and each genre searches for the titles of it on AniList, as the pages of AniList do
            val hashtag = text(media, "hashtag")
            val hashtagUrl = hashtag?.split(Regex("\\s+"))?.filter { it.startsWith("#") }?.takeIf { it.isNotEmpty() }
                ?.let { "$HashtagUrl${URLEncoder.encode(it.joinToString(" OR "), Charsets.UTF_8.name())}&src=typd" }
            add(R.string.title_data_hashtag, hashtag, hashtagUrl)
            val genres = strings(media.optJSONArray("genres"))
            addAll(R.string.title_data_genres, genres.map { genreNames[it] ?: it }, urls = genres.map { "$SearchUrl/${if (manga) "manga" else "anime"}/${Uri.encode(it)}" })
            add(R.string.title_data_romaji, titles?.let { text(it, "romaji") })
            add(R.string.title_data_english, titles?.let { text(it, "english") })
            add(R.string.title_data_native, titles?.let { text(it, "native") })
            addAll(R.string.title_data_synonyms, strings(media.optJSONArray("synonyms")))
        }
        val schedule = objects(media.optJSONObject("airingSchedule")?.optJSONArray("nodes")).mapNotNull { node ->
            if (node.isNull("airingAt")) null else NextAiring(node.optInt("episode"), node.getLong("airingAt"))
        }
        return TitleDetails(
            schedule = schedule,
            rankings = rankings(media),
            data = data,
            tags = tags(media.optJSONArray("tags")),
            trailer = media.optJSONObject("trailer")?.let(::trailer),
            links = links(media.optJSONArray("externalLinks")),
            relations = relations(media.optJSONObject("relations")?.optJSONArray("edges")),
            malId = media.optInt("idMal").takeIf { it > 0 },
            ageGroup = if (manga) mangaAge(media) else null
        )
    }

    // The titles tied to this one as cards with the name of the tie under them, the prequel and the sequel first and each kind in the order
    // AniList gives it, the same title once
    private fun relations(edges: JSONArray?): List<TitleRelation> {
        val order = resources.getStringArray(R.array.title_relation_keys).toList()
        return objects(edges)
            .mapNotNull { edge ->
                val type = text(edge, "relationType") ?: return@mapNotNull null
                val card = AniShelvesParser.card(edge.optJSONObject("node")) ?: return@mapNotNull null
                Triple(order.indexOf(type).let { if (it < 0) order.size else it }, card, type)
            }
            .sortedBy { it.first }
            .distinctBy { it.second.id }
            .map { (_, card, type) ->
                val label = relationNames[type] ?: type
                TitleRelation(mapper.card(card, details = label), label, type, card.banner ?: card.cover)
            }
    }

    // The places of the title among the best rated and the most popular, each as the words of AniList do: #81 most popular of all time. The
    // best places come first, and the ones with the same place are in the alphabetical order of the language of the phone
    private fun rankings(media: JSONObject): List<TitleRanking> {
        val collator = Collator.getInstance(Locale.getDefault())
        return objects(media.optJSONArray("rankings"))
            .mapNotNull { item ->
                val rank = number(item, "rank") ?: return@mapNotNull null
                val popular = item.optString("type") == PopularRanking
                val year = number(item, "year")
                val season = text(item, "season")?.let { seasonNames[it] }
                val scope = when {
                    item.optBoolean("allTime") -> resources.getString(R.string.title_rank_all_time)
                    year != null && season != null -> resources.getString(R.string.title_rank_in_season, season, year)
                    year != null -> resources.getString(R.string.title_rank_in_year, year)
                    else -> return@mapNotNull null
                }
                val base = resources.getString(if (popular) R.string.title_rank_popular else R.string.title_rank_rated)
                TitleRanking(rank, "#$rank $base $scope")
            }
            .sortedWith(Comparator { a, b -> if (a.rank != b.rank) a.rank.compareTo(b.rank) else collator.compare(a.text, b.text) })
            .take(MaxRankings)
    }

    // All the tags, the ones that give away the story too, in the alphabetical order of the language of the phone
    private fun tags(list: JSONArray?): List<TitleTag> {
        val collator = Collator.getInstance(Locale.getDefault())
        return objects(list)
            .mapNotNull { tag ->
                text(tag, "name")?.let {
                    TitleTag(it, tag.optInt("rank"), tag.optBoolean("isMediaSpoiler") || tag.optBoolean("isGeneralSpoiler"), text(tag, "description"))
                }
            }
            .sortedWith(compareBy(collator) { it.name })
    }

    // The trailer opens on the site that has it, null when that is not one that is known
    private fun trailer(json: JSONObject): TitleTrailer? {
        val id = text(json, "id") ?: return null
        val url = when (text(json, "site")) {
            "youtube" -> "https://www.youtube.com/watch?v=$id"
            "dailymotion" -> "https://www.dailymotion.com/video/$id"
            else -> return null
        }
        return TitleTrailer(url, text(json, "thumbnail"), id.takeIf { text(json, "site") == "youtube" })
    }

    // The links to the sites that have the title, the ones that show it and the ones about it, the ones that are off are left out
    private fun links(list: JSONArray?): List<TitleLink> {
        if (list == null) return emptyList()
        return (0 until list.length()).map { list.getJSONObject(it) }
            .filterNot { it.optBoolean("isDisabled") }
            .mapNotNull { link ->
                val site = text(link, "site")
                val url = text(link, "url")
                if (site == null || url == null) null else TitleLink(site, url, text(link, "type") == StreamingLink, text(link, "language"))
            }
    }

    // The gradient that shows until the picture arrives, from the color of the cover to the page
    private fun colors(hex: String?): List<Color> {
        val base = hex?.let { runCatching { Color(it.toColorInt()) }.getOrNull() } ?: ContainerHigh
        return listOf(base, lerp(base, Background, FadeToPage))
    }

    private fun names(keys: Int, names: Int): Map<String, String> =
        resources.getStringArray(keys).zip(resources.getStringArray(names)).toMap()

    // Turns or not the title among the favourites of the user, true when AniList did it. It only says what the title is now, so the page
    // keeps its own idea of it and goes back when this is false
    // The entry as AniList gives it, null when it has no state that is known
    private fun listEntry(json: JSONObject): ListEntry? {
        val status = ListStatus.of(text(json, "status")) ?: return null
        return ListEntry(
            id = json.optInt("id"),
            status = status,
            progress = json.optInt("progress"),
            progressVolumes = json.optInt("progressVolumes"),
            score = json.optDouble("score", 0.0).toFloat(),
            repeat = json.optInt("repeat"),
            notes = text(json, "notes").orEmpty(),
            isPrivate = json.optBoolean("private"),
            hidden = json.optBoolean("hiddenFromStatusLists"),
            startedAt = json.optJSONObject("startedAt")?.let(::localDate),
            completedAt = json.optJSONObject("completedAt")?.let(::localDate),
            customLists = json.optJSONObject("customLists")?.let { lists -> lists.keys().asSequence().filter { lists.optBoolean(it) }.toList() }.orEmpty(),
            advancedScores = json.optJSONObject("advancedScores")?.let { scores -> scores.keys().asSequence().associateWith { scores.optDouble(it, 0.0).toFloat() } }.orEmpty(),
            scoreOutOfTen = json.optInt("userScore") / ScoreTenScale
        )
    }

    // Puts the title on the list of the user or changes what is there, and gives the entry as AniList kept it. Null when it was not taken
    suspend fun saveEntry(id: Int, draft: ListDraft): ListEntry? {
        val variables = JSONObject()
            .put("mediaId", id)
            .put("status", draft.status.apiName)
            .put("progress", draft.progress)
            .put("progressVolumes", draft.progressVolumes)
            .put("score", draft.score.toDouble())
            .put("customLists", draft.customLists?.let { JSONArray(it) } ?: JSONObject.NULL)
            .put("advancedScores", draft.advancedScores?.let { scores -> JSONArray().apply { scores.forEach { put(it.toDouble()) } } } ?: JSONObject.NULL)
            .put("repeat", draft.repeat)
            .put("notes", draft.notes)
            .put("private", draft.isPrivate)
            .put("hidden", draft.hidden)
            .put("startedAt", fuzzyDate(draft.startedAt))
            .put("completedAt", fuzzyDate(draft.completedAt))
        return client.query(SaveEntryQuery, variables, AniListSession.token)?.optJSONObject("SaveMediaListEntry")?.let(::listEntry)
    }

    // A day as AniList takes it, and all of it empty when there is no day, which is how a day is taken away
    private fun fuzzyDate(day: LocalDate?): JSONObject =
        JSONObject()
            .put("year", day?.year ?: JSONObject.NULL)
            .put("month", day?.monthValue ?: JSONObject.NULL)
            .put("day", day?.dayOfMonth ?: JSONObject.NULL)

    // How the user gives a score and the lists they made for this kind of title, from their account, kept on the phone for a while. The
    // key has the name of the user, so another account does not get what the first one chose. Null when AniList could not be reached
    suspend fun listOptions(manga: Boolean): ListOptions? {
        val user = AniListSession.viewer?.name ?: return null
        val raw = kept(OptionsSpace, user) { client.query(OptionsQuery, JSONObject(), AniListSession.token)?.toString() } ?: return null
        val options = JSONObject(raw).optJSONObject("Viewer")?.optJSONObject("mediaListOptions") ?: return null
        val kind = options.optJSONObject(if (manga) "mangaList" else "animeList")
        return ListOptions(
            scoreFormat = ScoreFormat.of(text(options, "scoreFormat")),
            customLists = strings(kind?.optJSONArray("customLists")),
            advancedScoring = if (kind?.optBoolean("advancedScoringEnabled") == true) strings(kind.optJSONArray("advancedScoring")) else emptyList()
        )
    }

    // Takes the entry off the list, true when AniList did it
    suspend fun removeEntry(entryId: Int): Boolean =
        client.query(RemoveEntryQuery, JSONObject().put("id", entryId), AniListSession.token)?.optJSONObject("DeleteMediaListEntry")?.optBoolean("deleted") == true

    suspend fun toggleFavourite(id: Int, manga: Boolean): Boolean =
        client.query(ToggleFavouriteQuery, JSONObject().put(if (manga) "mangaId" else "animeId", id), AniListSession.token) != null

    private fun localDate(json: JSONObject): LocalDate? {
        val year = number(json, "year") ?: return null
        val month = number(json, "month") ?: return null
        val day = number(json, "day") ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    // A date as it is written in the language of the phone, all of it, the month and the year, or only the year, as much as AniList knows
    private fun dateText(json: JSONObject): String? {
        val year = number(json, "year") ?: return null
        val month = number(json, "month")
        val day = number(json, "day")
        val locale = Locale.getDefault()
        return runCatching {
            when {
                month != null && day != null -> LocalDate.of(year, month, day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                month != null -> YearMonth.of(year, month).format(DateTimeFormatter.ofPattern(MonthYearPattern, locale))
                else -> year.toString()
            }
        }.getOrNull()
    }

    private fun strings(list: JSONArray?): List<String> =
        if (list == null) emptyList() else (0 until list.length()).mapNotNull { list.optString(it).takeIf { text -> text.isNotBlank() } }

    private fun number(json: JSONObject, key: String): Int? = if (json.isNull(key)) null else json.optInt(key).takeIf { it > 0 }

    // A value that is null on the server comes as the text "null" with the plain getters, and a field that was not asked for is not there
    private fun text(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.getString(key).takeIf { it.isNotBlank() }

    internal companion object {
        const val MangaType = "MANGA"
        const val MovieFormat = "MOVIE"
        const val PrequelRelation = "PREQUEL"
        const val PopularRanking = "POPULAR"
        // How many places the highlights show, how many titles of a series are looked for at most and how many go in each question
        const val MaxRankings = 8
        const val MaxSeries = 40
        const val SeriesBatch = 50
        // The site of the order of an anime, asked by the id on MyAnimeList, what it wants to see in the request and what a row of its page
        // begins with. What it gave is kept for six hours
        const val ChiakiOrderUrl = "https://chiaki.site/?/tools/watch_order/id/"
        val ChiakiHeaders = mapOf(
            "Referer" to "https://chiaki.site/?/tools/watch_order",
            "X-Requested-With" to "XMLHttpRequest",
            "Accept" to "text/html"
        )
        val ChiakiRow = Regex("<tr[^>]*\\sdata-id=\"(\\d+)\"")
        const val OrderSpace = "title-order1"
        const val OrderMillis = 6L * 60L * 60L * 1000L
        const val MalCardsQuery = "query (\$ids: [Int]) { Page(perPage: 50) { media(idMal_in: \$ids, type: ANIME) { idMal ${AniShelvesParser.CardFields} } } }"
        const val MaxHops = 6
        const val AnimeType = "ANIME"
        // Where what AniList says of a title and of its series is kept on the phone, and for how long: half an hour, since it changes slowly
        const val ExtrasSpace = "title-extras13"
        const val SeriesSpace = "title-series3"
        // What a title of a series keeps in place of the series, the id of the title that has it
        const val SeriesPointer = "@"
        const val KeptMillis = 30L * 60L * 1000L
        // The ties that make the order of a series: before, after, apart from the story and from it
        val SeriesTies = setOf("PREQUEL", "SEQUEL", "SIDE_STORY", "SPIN_OFF", "PARENT")
        const val StreamingLink = "STREAMING"
        const val MonthYearPattern = "MMMM yyyy"
        // The place of a character and of a person of the staff on the site of AniList, and the most episodes a list shows
        const val CastCharactersQuery = "query (\$id: Int, \$page: Int) { Media(id: \$id) { countryOfOrigin characters(sort: [ROLE, RELEVANCE, FAVOURITES_DESC], page: \$page, perPage: 25) { " +
            "pageInfo { hasNextPage } edges { role node { id name { userPreferred } image { large } description(asHtml: false) } " +
            "voiceActors(sort: [RELEVANCE, ID]) { name { userPreferred } languageV2 } } } } }"
        const val CastStaffQuery = "query (\$id: Int, \$page: Int) { Media(id: \$id) { staff(sort: RELEVANCE, page: \$page, perPage: 25) { " +
            "pageInfo { hasNextPage } edges { role node { id name { userPreferred } image { large } description(asHtml: false) } } } } }"
        // The parts of the staff that are shown in the data, and how many of the people
        val CreatorRole = Regex("(?i)original creator|^story|^art|story & art|^director|chief director|series composition|character design|^music|original character")
        const val MaxCreators = 8
        const val CharacterPage = "character"
        const val StaffPage = "staff"
        const val MaxEpisodes = 2000
        // How many reviews the community shows
        const val MaxReviews = 5
        const val MaxGenres = 3
        const val FadeToPage = 0.7f
        // AniList scores go from 0 to 100, the page shows them from 0 to 10
        const val ScoreScale = 10f

        // What is asked of an entry of the list: the score is asked from 0 to 100, whatever the user chose to see on AniList
        // The score of AniList that is from 0 to 100, shown from 0 to 10
        const val ScoreTenScale = 10f
        const val EntryFields = "id status progress progressVolumes score userScore: score(format: POINT_100) repeat notes private hiddenFromStatusLists customLists advancedScores " +
            "startedAt { year month day } completedAt { year month day }"
        // What the top of the page and the strip under the synopsis need of a title. The filter is only there to make each question new
        const val HeaderQuery = "query (\$id: Int, \$fresh: Int) { Media(id: \$id, popularity_greater: \$fresh) { id idMal type status isAdult isFavourite format " +
            "title { romaji english } description(asHtml: false) startDate { year month day } genres averageScore meanScore " +
            "episodes chapters volumes duration season seasonYear studios { edges { isMain node { name isAnimationStudio } } } " +
            "coverImage { extraLarge color } bannerImage siteUrl " +
            "nextAiringEpisode { airingAt episode } mediaListEntry { $EntryFields } relations { edges { relationType } } } " +
            "friends: Page(perPage: 15) { mediaList(mediaId: \$id, isFollowing: true, sort: UPDATED_TIME_DESC) { " +
            "status progress score(format: POINT_10_DECIMAL) user { name avatar { large } } } } }"

        // Everything after the top in one question, as three names for the same title: the details (the data, the tags, the trailer, the links
        // and what is tied to it), what other people recommend, the ones with a score above zero being the ones that people agreed on, and the
        // characters, the best known first, with who voices them in Japanese, and the staff
        const val ExtrasQuery = "query (\$id: Int) { " +
            "details: Media(id: \$id) { idMal isAdult type format status title { romaji english native } synonyms episodes chapters volumes duration " +
            "season seasonYear startDate { year month day } endDate { year month day } source countryOfOrigin " +
            "studios { edges { isMain node { id name isAnimationStudio } } } popularity favourites " +
            "averageScore meanScore hashtag genres airingSchedule(notYetAired: true, perPage: 12) { nodes { episode airingAt } } rankings { rank type allTime year season } tags { name description rank isMediaSpoiler isGeneralSpoiler isAdult } " +
            "trailer { id site thumbnail } externalLinks { site url type language isDisabled } " +
            "relations { edges { relationType(version: 2) node { ${AniShelvesParser.CardFields} } } } } " +
            "recommended: Media(id: \$id) { recommendations(sort: RATING_DESC, perPage: 20) { nodes { rating mediaRecommendation { ${AniShelvesParser.CardFields} } } } } " +
            "cast: Media(id: \$id) { countryOfOrigin " +
            "characters(sort: [ROLE, RELEVANCE, FAVOURITES_DESC], perPage: 20) { pageInfo { hasNextPage } edges { role node { id name { userPreferred } image { large } } " +
            "voiceActors(sort: [RELEVANCE, ID]) { name { userPreferred } languageV2 } } } " +
            "staff(sort: RELEVANCE, perPage: 20) { pageInfo { hasNextPage } edges { role node { id name { userPreferred } image { large } } } } } }"

        // The titles of a series, each with when it started and what it is tied to, asked many at once
        const val WatchOrderQuery = "query (\$ids: [Int], \$type: MediaType) { Page(perPage: 50) { " +
            "media(id_in: \$ids, type: \$type) { ${AniShelvesParser.CardFields} startDate { year month day } " +
            "relations { edges { relationType(version: 2) node { id type } } } } } }"

        // How many days of activity the community shows
        const val TrendDays = 30

        // The community: how the scores and the lists are spread, the reviews and the threads about it
        const val CommunityQuery = "query (\$id: Int, \$fresh: Int) { " +
            "Media(id: \$id, popularity_greater: \$fresh) { popularity favourites stats { scoreDistribution { score amount } statusDistribution { status amount } } " +
            "trends(sort: ID_DESC, perPage: $TrendDays) { nodes { date trending } } " +
            "reviews(perPage: 5, sort: RATING_DESC) { nodes { id summary score rating ratingAmount createdAt user { name avatar { large } } } } } " +
            "threads: Page(perPage: 5) { threads(mediaCategoryId: \$id, sort: REPLIED_AT_DESC) { id title replyCount user { name } } } " +
            "activity: Page(perPage: 10) { activities(mediaId: \$id, type: MEDIA_LIST, sort: ID_DESC) { ... on ListActivity { id status progress createdAt user { name avatar { large } } } } } }"

        // A page of the reviews of a title, the most useful first, and one review with its text
        const val ReviewsQuery = "query (\$id: Int, \$page: Int) { Page(page: \$page, perPage: 20) { pageInfo { hasNextPage } " +
            "reviews(mediaId: \$id, sort: RATING_DESC) { id summary score rating ratingAmount createdAt user { name avatar { large } } } } }"
        const val ReviewQuery = "query (\$id: Int) { Review(id: \$id) { id body(asHtml: false) summary score rating ratingAmount userRating createdAt user { name avatar { large } } } }"

        const val RateReviewQuery = "mutation (\$id: Int, \$rating: ReviewRating) { RateReview(reviewId: \$id, rating: \$rating) { rating ratingAmount userRating } }"

        // Puts a title on the list of the user or changes its entry, and takes it off
        const val SaveEntryQuery = "mutation (\$mediaId: Int, \$status: MediaListStatus, \$progress: Int, \$progressVolumes: Int, \$score: Float, " +
            "\$customLists: [String], \$advancedScores: [Float], \$repeat: Int, \$notes: String, \$private: Boolean, \$hidden: Boolean, \$startedAt: FuzzyDateInput, \$completedAt: FuzzyDateInput) { " +
            "SaveMediaListEntry(mediaId: \$mediaId, status: \$status, progress: \$progress, progressVolumes: \$progressVolumes, score: \$score, " +
            "customLists: \$customLists, advancedScores: \$advancedScores, " +
            "repeat: \$repeat, notes: \$notes, private: \$private, hiddenFromStatusLists: \$hidden, startedAt: \$startedAt, completedAt: \$completedAt) { $EntryFields } }"
        const val RemoveEntryQuery = "mutation (\$id: Int) { DeleteMediaListEntry(id: \$id) { deleted } }"
        const val SearchUrl = "https://anilist.co/search"
        const val HashtagUrl = "https://x.com/search?q="
        const val WeightedScoreUrl = "https://anilist.co/forum/thread/89398"
        const val OptionsQuery = "{ Viewer { mediaListOptions { scoreFormat " +
            "animeList { customLists advancedScoring advancedScoringEnabled } mangaList { customLists advancedScoring advancedScoringEnabled } } } }"
        const val OptionsSpace = "list-options1"

        // The favourite of a title, which AniList turns on or off, and something of the answer has to be asked for
        const val ToggleFavouriteQuery = "mutation (\$animeId: Int, \$mangaId: Int) { " +
            "ToggleFavourite(animeId: \$animeId, mangaId: \$mangaId) { anime { pageInfo { total } } } }"
    }
}
