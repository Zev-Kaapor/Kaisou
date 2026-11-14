package com.kaisou.app.features.home.data

// Image loading
import coil3.size.Size
// UI styling and utilities
import androidx.compose.ui.graphics.Color
// Java time
import java.time.DayOfWeek
import java.time.LocalDate
// Project resources
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

// What the Home is doing: loading with nothing to show, showing what came, or failed with nothing to show
enum class HomeStatus { Loading, Content, Error }

data class HomeUiState(
    val status: HomeStatus = HomeStatus.Loading,
    val heroes: List<HeroUi> = emptyList(),
    // What the look for the pictures and the translation of each title came to, by AniList id. A title that is not here is still
    // being looked for and shows as a placeholder
    val art: Map<Int, HeroArt> = emptyMap(),
    // The featured title that is on. It is here and not in the screen, which is built in another way standing up and lying down,
    // so turning the phone does not change the title
    val heroIndex: Int = 0,
    // Sizes the poster, the backdrop and the logo are downloaded at, the page asks for the same ones so it finds them in memory
    val posterSize: Size = Size.ORIGINAL,
    val backdropSize: Size = Size.ORIGINAL,
    val logoSize: Size = Size.ORIGINAL
)
