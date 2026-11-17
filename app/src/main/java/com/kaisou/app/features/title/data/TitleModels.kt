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
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.data.HeroArt

// The states a title can have on the list of the user, as AniList names them
enum class ListStatus(val apiName: String) {
    Current("CURRENT"), Planning("PLANNING"), Completed("COMPLETED"), Repeating("REPEATING"), Paused("PAUSED"), Dropped("DROPPED");

    companion object {
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

// The entry of a title on the list of the user: where it is, how far they are in chapters or episodes and in volumes, the score from 0 to
// 10, how many times it was done again, their notes, whether it is private or hidden from the lists of its status, and the days it started
// and ended
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
    val completedAt: LocalDate?
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
    val completedAt: LocalDate?
)

// Where a title is in its life, as AniList names it. It is what the releases that the user can follow depend on: a title that is out
// has its next episode or chapter, one that is done may get a new season, one that stopped may come back, one that is coming has a start
enum class ReleaseState(@StringRes val label: Int) {
    Releasing(R.string.title_status_releasing),
    Finished(R.string.title_status_finished),
    Hiatus(R.string.title_status_hiatus),
    NotYetReleased(R.string.title_status_not_yet),
    Cancelled(R.string.title_status_cancelled);

    companion object {
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
    // Where the title is on the list of the user, null when it is not on it, and the episode or chapter that comes next: the one after
    // the last the user did, or the first one
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
    val siteUrl: String?
)

// One line of the data of a title, the name of what it is and what it says. A line that says several things, like the names it goes by,
// has one for each, so a name with a comma in it is not taken for two
data class TitleDatum(val label: String, val values: List<String>)

// A place of the title among the best rated or the most popular: its number, which colors its star, and the words that say it
data class TitleRanking(val rank: Int, val text: String)

// A tag of a title with how much of it people say it has, and whether it gives away the story
data class TitleTag(val name: String, val rank: Int, val spoiler: Boolean, val description: String?)

// The trailer of a title: where it opens and a picture of it
data class TitleTrailer(val url: String, val thumbnail: String?, val youtubeId: String?)

// A link of a title to another site, with what the site is and what the link is for
data class TitleLink(val site: String, val url: String, val streaming: Boolean, val language: String?)

// A title that is tied to this one, a sequel or a prequel or what it was made from, as a card with the name of the tie, the kind of tie
// as AniList names it and a wide picture of the title
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
    val rankings: List<TitleRanking>,
    val data: List<TitleDatum>,
    val tags: List<TitleTag>,
    val trailer: TitleTrailer?,
    val links: List<TitleLink>,
    val relations: List<TitleRelation>
)

// Someone in the title, a character or a person of the staff: the name, the picture, what they do in it and, for a character, the voice
data class TitlePerson(val name: String, val image: String?, val role: String?, val voice: String?, val url: String)

// The characters and the staff of a title
data class TitlePeople(val characters: List<TitlePerson>, val staff: List<TitlePerson>)

// An episode of an anime: its number, its name, a picture of it, how long it is in minutes and the day it aired as the phone writes it
data class TitleEpisode(val number: Int, val title: String?, val image: String?, val minutes: Int?, val aired: String?)

// A friend that has the title on a list, with where it is, how far and the score on the scale of ten
data class TitleFriend(val name: String, val avatar: String?, val status: String, val progress: Int?, val score: Float?, val url: String)

// A review of the title: who wrote it, the score they gave, how it starts and how many people found it useful
data class TitleReview(val id: Int, val author: String, val avatar: String?, val score: Int?, val summary: String, val helpful: Int, val url: String)

// A thread of the forum about the title
data class TitleThread(val title: String, val replies: Int, val author: String, val url: String)

// How many people gave a score, and how many have the title in a state
data class TitleScoreBar(val score: Int, val amount: Int)
data class TitleStatusShare(val label: String, val amount: Int)

// What the tab of the community of a title shows
data class TitleCommunity(
    val friends: List<TitleFriend>,
    val reviews: List<TitleReview>,
    val scores: List<TitleScoreBar>,
    val statuses: List<TitleStatusShare>,
    val threads: List<TitleThread>
)

// What the page is doing: loading with nothing to show, showing the title, or failed
enum class TitleStatus { Loading, Content, Error }

// A part of the page that is asked apart from the top: not asked yet, being asked, there, or failed
sealed interface Load<out T> {
    data object Idle : Load<Nothing>
    data object Loading : Load<Nothing>
    data class Done<T>(val value: T) : Load<T>
    data object Failed : Load<Nothing>
}

data class TitleUiState(
    val status: TitleStatus = TitleStatus.Loading,
    val title: TitleUi? = null,
    // What was found for the title besides its data: the pictures, the synopsis in the language of the app, the rating and the season
    val art: HeroArt? = null,
    // The parts that come after the top, each asked apart so a slow or failed one does not hold the others: the details, what other
    // people recommend, the characters and the staff, the episodes, and the community. The last two are only asked when their tab opens
    val details: Load<TitleDetails> = Load.Loading,
    val recommendations: Load<List<CardUi>> = Load.Loading,
    val watchOrder: Load<TitleWatchOrder> = Load.Loading,
    val people: Load<TitlePeople> = Load.Loading,
    val episodes: Load<List<TitleEpisode>> = Load.Idle,
    val community: Load<TitleCommunity> = Load.Idle,
    // The openings and the endings, and the news, which come from other sites and are only shown when they are there
    val themes: Load<List<TitleTheme>> = Load.Idle,
    // The covers of the volumes of a manga, empty for an anime or when none was found
    val covers: List<MangaCover> = emptyList(),
    val news: Load<List<TitleNews>> = Load.Idle,
    // Sizes the poster, the backdrop and the logo are downloaded at, the page asks for the same ones so it finds them in memory
    val posterSize: Size = Size.ORIGINAL,
    val backdropSize: Size = Size.ORIGINAL,
    val logoSize: Size = Size.ORIGINAL
)
