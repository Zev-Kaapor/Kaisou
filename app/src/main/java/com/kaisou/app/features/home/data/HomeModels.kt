package com.kaisou.app.features.home.data

// Image loading
import androidx.annotation.StringRes
import coil3.size.Size
// UI styling and utilities
import androidx.compose.ui.graphics.Color
// Java time
import java.time.DayOfWeek
import java.time.LocalDate
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.ArtQuery
import com.kaisou.app.core.rating.ContentRating

// How many featured titles the Home shows, drawn from the most trending ones of the season
const val HeroCount = 6

// Featured title at the top of the Home
data class HeroUi(
    val anilistId: Int,
    // The id on MyAnimeList, which gives the rating when the country does not, and whether the title is for adults
    val malId: Int?,
    val adult: Boolean,
    // The season its name says, if it does, and whether another title comes before it in the series
    val seasonMarker: Int?,
    val hasPrequel: Boolean,
    // The day it started, which tells its season by the days the seasons of the series aired
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
    val nextEpisode: Int,
    // The user is in the middle of it, so the button continues where they stopped
    val continuing: Boolean,
    // Cover of AniList, the picture of the title until a better one is found
    val coverUrl: String?,
    // Gradient that shows until the picture arrives, from the color of the cover to the page
    val artColors: List<Color>
)

// What is ready for a featured title besides its data: the poster and the backdrop, the wide picture for the screen lying down, and
// the logo that goes in place of its name. The one of the way the screen is in was downloaded, the other is only known,
// its synopsis in the language of the app, its advisory rating for the country of the user and its season in the series. Each is
// null when there is none, and the cover, the name in text and the synopsis as it came take its place, with no rating no tag
// shows and with no season the button only has the episode
class HeroArt(
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val synopsis: String?,
    val rating: ContentRating?,
    val season: Int?
)

// The shelves of posters and wide cards under the hero, in the order they show, and the words that name each one
enum class ShelfId(@StringRes val titleRes: Int) {
    Watching(R.string.home_shelf_watching),
    Reading(R.string.home_shelf_reading),
    NewEpisodes(R.string.home_shelf_new_episodes),
    PlannedAnime(R.string.home_shelf_planned_anime),
    PlannedManga(R.string.home_shelf_planned_manga),
    MissingSequels(R.string.home_shelf_missing_sequels),
    RecommendedAnime(R.string.home_shelf_recommended_anime),
    RecommendedManga(R.string.home_shelf_recommended_manga),
    Trending(R.string.home_shelf_trending),
    TrendingManga(R.string.home_shelf_trending_manga),
    Popular(R.string.home_shelf_popular),
    Upcoming(R.string.home_shelf_upcoming),
    NewAnime(R.string.home_shelf_new_anime),
    NewManga(R.string.home_shelf_new_manga)
}

// The shape of the cards of a shelf: a poster standing up, or a wide picture
enum class CardShape { Poster, Wide }

// A title on a shelf: its picture, its score from 0 to 10, null when it has none, the
// number of an episode that just aired, and the line under its name: how far the user is in it, as done out of the total, or the
// details of the title. A wide card also looks for the wide picture of its title, to show it in turn with the one it has
data class CardUi(
    val anilistId: Int,
    val title: String,
    val imageUrl: String?,
    val score: Float?,
    val episode: Int?,
    val done: Int?,
    val total: Int?,
    val details: String,
    // What the other sites are asked by to find a better picture of the title, only for the cards that look for one
    val query: ArtQuery?
)

// A shelf of the Home: what it is, the shape of its cards and the titles on it
data class ShelfUi(val id: ShelfId, val shape: CardShape, val cards: List<CardUi>)

// One thing a person did, as the story shows it: the line that says it, or the text they wrote, and the pictures of the title
data class StoryItemUi(
    val id: Int,
    // Seconds
    val createdAt: Long,
    val line: String?,
    // The part of the line that is shown in the color of the app: the episodes or chapters, or the title when there are none
    val highlight: IntRange?,
    val text: String?,
    val coverUrl: String?,
    val backdropUrl: String?,
    val likeCount: Int,
    val replyCount: Int,
    val isLiked: Boolean
)

// A comment on an activity
data class ReplyUi(
    val id: Int,
    val userId: Int,
    val userName: String,
    val avatarUrl: String?,
    val text: String,
    val createdAt: Long,
    val likeCount: Int,
    val isLiked: Boolean
)

// The comments of the activity that is open: what is known of them, and whether they are coming or one is being sent or failed
data class RepliesUi(
    val activityId: Int,
    val items: List<ReplyUi> = emptyList(),
    val loading: Boolean = true,
    val sending: Boolean = false,
    val failed: Boolean = false,
    // The comment whose text is being changed, null when a new one is being written
    val editingId: Int? = null
)

// The sheet where the user writes a status: whether it is being posted, whether it failed, and whether it was taken, which closes it
data class ComposerUi(val sending: Boolean = false, val failed: Boolean = false, val sent: Boolean = false)

// A person with what they did lately, the oldest first, which is the order the story plays
data class StoryUi(val userId: Int, val name: String, val avatarUrl: String?, val items: List<StoryItemUi>)

// Everything under the hero: what the user did lately, the friends and the shelves
data class HomeShelvesUi(val ownStory: List<StoryItemUi>, val friends: List<StoryUi>, val shelves: List<ShelfUi>)

// What the Home is doing: loading with nothing to show, showing what came, or failed with nothing to show
enum class HomeStatus { Loading, Content, Error }

data class HomeUiState(
    val status: HomeStatus = HomeStatus.Loading,
    // Asking again with the Home on the screen, which stays until the new one comes
    val isRefreshing: Boolean = false,
    val heroes: List<HeroUi> = emptyList(),
    // What the look for the pictures and the translation of each title came to, by AniList id. A title that is not here is still
    // being looked for and shows as a placeholder
    val art: Map<Int, HeroArt> = emptyMap(),
    // The featured title that is on. It is here and not in the screen, which is built in another way standing up and lying down,
    // so turning the phone does not change the title
    val heroIndex: Int = 0,
    // What is under the hero, which comes with it: nothing is shown until both are ready
    val ownStory: List<StoryItemUi> = emptyList(),
    val friends: List<StoryUi> = emptyList(),
    // The ids of the activities the user already saw, kept on the device
    val seen: Set<Int> = emptySet(),
    // The comments of the activity of a story, null while none is open
    val replies: RepliesUi? = null,
    // The sheet to write a status, null while it is closed
    val composer: ComposerUi? = null,
    val shelves: List<ShelfUi> = emptyList(),
    // Sizes the poster, the backdrop and the logo are downloaded at, the page asks for the same ones so it finds them in memory
    val posterSize: Size = Size.ORIGINAL,
    val backdropSize: Size = Size.ORIGINAL,
    val logoSize: Size = Size.ORIGINAL
)
