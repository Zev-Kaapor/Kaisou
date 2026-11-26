package com.kaisou.app.features.title.data

// Android resources
import androidx.annotation.StringRes
// Image loading
import coil3.size.Size
// UI styling and utilities
import androidx.compose.ui.graphics.Color
// Java time
import java.time.DayOfWeek
import java.time.LocalDate
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.ArtQuery
import com.kaisou.app.core.artwork.TitleGallery
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.home.data.HeroArt
import com.kaisou.app.core.rating.AgeGroup

// The states a title can have on the list of the user, as AniList names them
enum class ListStatus(val apiName: String) {
    Current("CURRENT"), Planning("PLANNING"), Completed("COMPLETED"), Repeating("REPEATING"), Paused("PAUSED"), Dropped("DROPPED");

    // Finds a list status by its name
    companion object {
        // Gives the status of a name, null when it is not known
        fun of(name: String?): ListStatus? = when (name) {
            "CURRENT" -> Current
            "PLANNING" -> Planning
            "COMPLETED" -> Completed
            "REPEATING" -> Repeating
            "PAUSED" -> Paused
            "DROPPED" -> Dropped
            else -> null
        }
    }
}

// The ways the user can give a score on AniList, from 0 to 100 down to three faces, the score of an entry being in the user's way
enum class ScoreFormat(val apiName: String) {
    Point100("POINT_100"), Point10Decimal("POINT_10_DECIMAL"), Point10("POINT_10"), Point5("POINT_5"), Point3("POINT_3");

    // Finds a score format by its name
    companion object {
        // Gives the score format of a name, ten with a decimal when it is not known
        fun of(name: String?): ScoreFormat = entries.firstOrNull { it.apiName == name } ?: Point10Decimal
    }
}

// What the account says about the lists of a kind of title: how the score is given, the lists made and the detailed score parts
data class ListOptions(
    val scoreFormat: ScoreFormat = ScoreFormat.Point10Decimal,
    val customLists: List<String> = emptyList(),
    val advancedScoring: List<String> = emptyList()
)

// The entry of a title on the user's list: place, progress, score, repeats, notes, privacy, dates, own lists and detailed score
data class ListEntry(
    val id: Int,
    val status: ListStatus,
    val progress: Int,
    val progressVolumes: Int,
    val score: Float,
    val repeat: Int,
    val notes: String,
    val isPrivate: Boolean,
    val hidden: Boolean,
    val startedAt: LocalDate?,
    val completedAt: LocalDate?,
    val customLists: List<String> = emptyList(),
    val advancedScores: Map<String, Float> = emptyMap(),
    // The score from 0 to 10 whatever way the account gives them, 0 when there is none, to show next to the one of AniList
    val scoreOutOfTen: Float = 0f
) {
    // The episode or chapter that comes next: the one after the last the user did, or the first when it is not started or is done
    val nextUnit: Int
        get() = if (status == ListStatus.Completed || status == ListStatus.Planning) 1 else progress + 1
}

// What the user chose in the editor of the list, before it is sent to AniList
data class ListDraft(
    val status: ListStatus,
    val progress: Int,
    val progressVolumes: Int,
    val score: Float,
    val repeat: Int,
    val notes: String,
    val isPrivate: Boolean,
    val hidden: Boolean,
    val startedAt: LocalDate?,
    val completedAt: LocalDate?,
    // Null when the lists of the user or their detailed score are not known, so they are not sent and nothing is taken away
    val customLists: List<String>? = null,
    val advancedScores: List<Float>? = null
)

// Where a title is in its life as AniList names it, which decides the releases the user can follow
enum class ReleaseState(@StringRes val label: Int) {
    Releasing(R.string.title_status_releasing),
    Finished(R.string.title_status_finished),
    Hiatus(R.string.title_status_hiatus),
    NotYetReleased(R.string.title_status_not_yet),
    Cancelled(R.string.title_status_cancelled);

    // Finds a release state by its name
    companion object {
        // Gives the release state of a name, null when it is not known
        fun of(name: String?): ReleaseState? = when (name) {
            "RELEASING" -> Releasing
            "FINISHED" -> Finished
            "HIATUS" -> Hiatus
            "NOT_YET_RELEASED" -> NotYetReleased
            "CANCELLED" -> Cancelled
            else -> null
        }
    }
}

// The next episode of a title that is airing: which one it is and when it airs, in seconds
data class NextAiring(val episode: Int, val airingAt: Long)

// A title as the top of its page shows it. Prototype: it holds what the top of the page and the strip under it need
data class TitleUi(
    val anilistId: Int,
    val manga: Boolean,
    // The id on MyAnimeList, which gives the rating when the country does not, and whether the title is for adults
    val malId: Int?,
    val adult: Boolean,
    // The season its name says, if it does, and whether another title comes before it in the series
    val seasonMarker: Int?,
    // The part of the season its name says, as 2 for Part 2, null when it does not say one
    val partMarker: Int?,
    val isContinuation: Boolean,
    val hasPrequel: Boolean,
    val startDate: LocalDate?,
    val title: String,
    // What the sites that have its poster are asked by
    val query: ArtQuery,
    val format: String,
    val genres: String,
    val year: Int?,
    val synopsis: String,
    // Day of the week of a new episode, null when the title is not airing weekly
    val releaseDay: DayOfWeek?,
    // The score from 0 to 10, null when AniList has none
    val score: Float?,
    // Where the title is on the user's list, null when it is not, and the episode or chapter that comes next
    val listStatus: ListStatus?,
    val nextUnit: Int,
    val entry: ListEntry?,
    val isFavourite: Boolean,
    // Where the title is in its life, and the next episode when it is airing
    val releaseState: ReleaseState?,
    val nextAiring: NextAiring?,
    // What the strip under the synopsis says of it: how many episodes or chapters, how long an episode is, the season and the studio
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val duration: Int?,
    val seasonText: String?,
    val studio: String?,
    // The pictures of AniList, the ones of the title until better ones are found, and the gradient that shows until they arrive
    val coverUrl: String?,
    val bannerUrl: String?,
    val artColors: List<Color>,
    val siteUrl: String?,
    // The friends that have the title on a list, asked with the top of the page so they are as new as the heart and the list
    val friends: List<TitleFriend> = emptyList()
)

// One line of the data of a title, its name and what it says, a line with several things having one for each
data class TitleDatum(
    val label: String,
    val values: List<String>,
    // A value that opens a page of the app, or an address in the browser, by its place in the values
    val refs: List<ProfileRef?> = emptyList(),
    val urls: List<String?> = emptyList(),
    val help: DatumHelp? = null
)

// What a small question mark next to the name of a datum says when it is pressed and held, and where it goes when it is touched
data class DatumHelp(val text: String, val url: String)

// A place of the title among the best rated or the most popular: its number, which colors its star, and the words that say it
data class TitleRanking(val rank: Int, val text: String)

// A tag of a title with how much of it people say it has, and whether it gives away the story
data class TitleTag(val name: String, val rank: Int, val spoiler: Boolean, val description: String?)

// The trailer of a title: where it opens and a picture of it
data class TitleTrailer(val url: String, val thumbnail: String?, val youtubeId: String?)

// A link of a title to another site, with what the site is and what the link is for
data class TitleLink(val site: String, val url: String, val streaming: Boolean, val language: String?)

// A title tied to this one, as a card with the name of the tie, its type as AniList names it and a wide picture
data class TitleRelation(val card: CardUi, val label: String, val type: String, val image: String?)

// The titles of the series in the order they came out, with the place of this one in it
data class TitleWatchOrder(val cards: List<CardUi>, val current: Int)

// An opening or an ending: which one it is (OP1, ED2), the song, who sings it and the episodes it plays in
data class TitleTheme(val label: String, val song: String?, val artist: String?, val episodes: String?)

// A piece of news about the title: the headline, the day, who wrote it, how it starts, a picture and where it opens
data class TitleNews(val title: String, val date: String?, val author: String?, val excerpt: String?, val image: String?, val url: String)

// What comes after the top, asked together
class TitleExtras(val details: TitleDetails, val recommendations: List<CardUi>, val people: TitlePeople)

// What the tab of the details of a title shows, which comes after the top
data class TitleDetails(
    // The episodes that are still to air, the next one first
    val schedule: List<NextAiring> = emptyList(),
    val rankings: List<TitleRanking>,
    val data: List<TitleDatum>,
    val tags: List<TitleTag>,
    val trailer: TitleTrailer?,
    val links: List<TitleLink>,
    val relations: List<TitleRelation>,
    // The id of the title on MyAnimeList, which the watch order of the anime is asked by
    val malId: Int? = null,
    // The age a manga is for, worked out from its tags and genres, null for an anime or a manga with nothing known
    val ageGroup: AgeGroup? = null
)

// Who voices a character and the language of the voice, as AniList names it, like Japanese or Portuguese
data class TitleVoice(val name: String, val language: String)

// Someone in the title, a character or staff: name, picture, what they do and a second line, the voice for a character
data class TitlePerson(
    val name: String,
    val image: String?,
    val role: String?,
    val voice: String?,
    val url: String,
    val ref: ProfileRef? = null,
    val language: String? = null,
    // The beginning of what a character says of itself, in the markdown of AniList, for the page of all the characters
    val about: String? = null
)


// A page of the characters or of the staff of a title, and whether there are more
class CastPage(val people: List<TitlePerson>, val hasMore: Boolean)

// The characters and staff of a title, its creators to open from the data, and whether AniList has more of them
data class TitlePeople(
    val characters: List<TitlePerson>,
    val staff: List<TitlePerson>,
    val creators: List<TitlePerson> = emptyList(),
    val moreCharacters: Boolean = false,
    val moreStaff: Boolean = false
)

// The kinds of picture in the gallery of an anime, each with the word in its address and the name it has on the screen
enum class GalleryKind(val path: String, @StringRes val label: Int) {
    Posters("posters", R.string.title_section_posters),
    Backdrops("backdrops", R.string.title_section_backdrops),
    Logos("logos", R.string.title_section_logos);

    // The pictures of this kind in a gallery
    fun of(gallery: TitleGallery) = when (this) {
        Posters -> gallery.posters
        Backdrops -> gallery.backdrops
        Logos -> gallery.logos
    }

    // Finds a kind by the word in the address, the posters when it is not one
    companion object {
        // The kind of a word in the address
        fun byPath(path: String?): GalleryKind = entries.firstOrNull { it.path == path } ?: Posters
    }
}

// An episode of an anime: number, name, picture, length, day, text, score, season, and whether its texts are in the user's language
data class TitleEpisode(
    val number: Int,
    val title: String?,
    val image: String?,
    val minutes: Int?,
    val aired: String?,
    val overview: String? = null,
    val rating: Float? = null,
    val season: Int? = null,
    val localized: Boolean = false
)

// The episode that comes next, only for a title that the user is going through
val TitleUi.upNext: Int?
    get() = nextUnit.takeIf { listStatus == ListStatus.Current || listStatus == ListStatus.Repeating || listStatus == ListStatus.Paused }

// The episodes of a title and the ids of its series on TMDB, IMDb, MyAnimeList and TheTVDB, where texts and scores come from
data class EpisodeSet(val episodes: List<TitleEpisode>, val tmdbId: Int?, val imdbId: String? = null, val malId: Int? = null, val tvdbId: Int? = null)

// A part of the list of episodes the user picks: a season, or a range of a series that is long and has no seasons
data class EpisodeGroup(val season: Int?, val first: Int, val last: Int, val episodes: List<TitleEpisode>)

// Cuts the episodes in seasons or ranges
object EpisodeGroups {
    // How many episodes a range has with no seasons, as Dantotsu: 25 up to 249, 50 up to 499, 100 after, a short series not cut
    private fun rangeSize(total: Int): Int = when {
        total < 250 -> 25
        total < 500 -> 50
        else -> 100
    }

    // The seasons when there are two or more, the ranges when there are more than one, else all in one, the unaired with the last
    fun of(episodes: List<TitleEpisode>): List<EpisodeGroup> {
        if (episodes.isEmpty()) return emptyList()
        val seasons = episodes.mapNotNull { it.season?.takeIf { season -> season > 0 } }.distinct().sorted()
        if (seasons.size > 1) {
            return seasons.mapIndexed { index, season ->
                val part = episodes.filter { (it.season ?: seasons.last()) == season || (it.season == 0 && index == 0) }
                EpisodeGroup(season, part.first().number, part.last().number, part)
            }
        }
        return episodes.chunked(rangeSize(episodes.size)).map { EpisodeGroup(null, it.first().number, it.last().number, it) }
    }

    // The group that is open: the one the user picked, otherwise the one with the episode that comes next, otherwise the first
    fun open(groups: List<EpisodeGroup>, picked: Int?, next: Int?): Int =
        (picked ?: indexOf(groups, next ?: groups.first().first)).coerceIn(groups.indices)

    // The group that has the episode, the first when none has it
    fun indexOf(groups: List<EpisodeGroup>, number: Int): Int =
        groups.indexOfFirst { group -> group.episodes.any { it.number == number } }.coerceAtLeast(0)
}

// A friend that has the title on a list, with where it is, how far and the score on the scale of ten
data class TitleFriend(
    val name: String,
    val avatar: String?,
    val status: String,
    val progress: Int?,
    val score: Float?,
    val url: String,
    // Whether it is the user, who comes first on the row
    val isMe: Boolean = false
)

// A review of the title: who wrote it, the score they gave, how it starts and how many people found it useful
data class TitleReview(
    val id: Int,
    val author: String,
    val avatar: String?,
    val score: Int?,
    val summary: String,
    val helpful: Int,
    val url: String,
    // How many people voted, how many found it useful, and the day it was written as seconds, 0 when it is not known
    val votes: Int = 0,
    val createdAt: Long = 0
)

// A page of the reviews of a title, and whether there are more
class ReviewsPage(val reviews: List<TitleReview>, val hasMore: Boolean)

// What the user voted on a review: nothing, the thumb up or the thumb down
enum class ReviewVote { None, Up, Down }

// A review with its whole text and the vote of the user
data class ReviewBody(val review: TitleReview, val text: String, val vote: ReviewVote = ReviewVote.None)

// How a review stands after a vote, as AniList counts it
class ReviewTally(val helpful: Int, val votes: Int, val vote: ReviewVote)

// A thread of the forum about the title
data class TitleThread(
    val id: Int,
    val title: String,
    val replies: Int,
    val author: String,
    val avatar: String?,
    val likes: Int,
    // The day it was written as seconds
    val createdAt: Long,
    val url: String
)

// A page of the threads of a title, and whether there are more
class ThreadsPage(val threads: List<TitleThread>, val hasMore: Boolean)

// A tag of a thread: its category or a title it is about, which opens the title
data class ThreadTag(val label: String, val mediaId: Int? = null)

// A thread with its whole text, whether the user liked it and follows it, whether it is locked and its tags
data class ThreadBody(
    val thread: TitleThread,
    val text: String,
    val liked: Boolean,
    val subscribed: Boolean = false,
    val locked: Boolean = false,
    val tags: List<ThreadTag> = emptyList()
)

// A comment of a thread with the answers to it, and how many liked it
data class ThreadComment(
    val id: Int,
    val author: String,
    val avatar: String?,
    val text: String,
    val likes: Int,
    val liked: Boolean,
    val createdAt: Long,
    val replies: List<ThreadComment> = emptyList()
)

// A page of the comments of a thread, and whether there are more
class CommentsPage(val comments: List<ThreadComment>, val hasMore: Boolean)

// How many people gave a score, and how many have the title in a state
data class TitleScoreBar(val score: Int, val amount: Int)

// How many people have the title in a state, by its label
data class TitleStatusShare(val label: String, val amount: Int)

// How much happened around the title on a day, as AniList counts it: the day as seconds and the activity of that day
data class TitleTrend(val day: Long, val activity: Int)

// What a person did to the title lately: who, what they did in the language of the app, and when as seconds
data class TitleActivity(val id: Int, val author: String, val avatar: String?, val text: String, val createdAt: Long)

// What the tab of the community of a title shows
data class TitleCommunity(
    val reviews: List<TitleReview>,
    val scores: List<TitleScoreBar>,
    val statuses: List<TitleStatusShare>,
    val threads: List<TitleThread>,
    // The last days of activity, from the oldest to the newest
    val trends: List<TitleTrend> = emptyList(),
    // How many people have the title on a list and how many made it a favourite
    val popularity: Int = 0,
    val favourites: Int = 0,
    // What people did to the title lately, the newest first
    val activity: List<TitleActivity> = emptyList()
)

// What the page is doing: loading with nothing to show, showing the title, or failed
enum class TitleStatus { Loading, Content, Error }

// A part of the page that is asked apart from the top: not asked yet, being asked, there, or failed
sealed interface Load<out T> {
    // Nothing has been asked yet
    data object Idle : Load<Nothing>

    // It is being asked
    data object Loading : Load<Nothing>

    // It came, with its value
    data class Done<T>(val value: T) : Load<T>

    // It failed
    data object Failed : Load<Nothing>
}

// What the page of a title shows: its state, the title and the parts that come after the top
data class TitleUiState(
    val status: TitleStatus = TitleStatus.Loading,
    val title: TitleUi? = null,
    // What was found for the title besides its data: the pictures, the synopsis in the language of the app, the rating and the season
    val art: HeroArt? = null,
    // The parts after the top, each asked apart so a slow one does not hold the others, the last two when their tab opens
    val details: Load<TitleDetails> = Load.Loading,
    val recommendations: Load<List<CardUi>> = Load.Loading,
    val watchOrder: Load<TitleWatchOrder> = Load.Loading,
    val people: Load<TitlePeople> = Load.Loading,
    val episodes: Load<List<TitleEpisode>> = Load.Idle,
    val community: Load<TitleCommunity> = Load.Idle,
    // The openings and the endings, and the news, which come from other sites and are only shown when they are there
    val themes: Load<List<TitleTheme>> = Load.Idle,
    // How the user gives a score and what lists they made, null until the account answers
    val listOptions: ListOptions? = null,
    // The covers of the volumes of a manga, empty for an anime or when none was found
    val covers: List<MangaCover> = emptyList(),

    // The posters, backdrops and logos of an anime from TMDB and TheTVDB, empty for a manga or until they come
    val gallery: TitleGallery = TitleGallery.None,
    val news: Load<List<TitleNews>> = Load.Idle,
    // Sizes the poster, the backdrop and the logo are downloaded at, the page asks for the same ones so it finds them in memory
    val posterSize: Size = Size.ORIGINAL,
    val backdropSize: Size = Size.ORIGINAL,
    val logoSize: Size = Size.ORIGINAL
)
