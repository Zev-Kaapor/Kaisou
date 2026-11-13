package com.kaisou.app.features.home.data

// Image loading
import coil3.size.Size
// Android annotations
import androidx.annotation.StringRes
// UI styling and utilities
import androidx.compose.ui.graphics.Color
// Java time
import java.time.DayOfWeek
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.HeroArt
import com.kaisou.app.core.model.ContentRating

// Featured title at the top of the Home
data class HeroUi(
    val anilistId: Int,
    // Id on MyAnimeList, where the advisory rating comes from when the artwork does not bring the official one
    val malId: Int?,
    val title: String,
    // Every name the title goes by and whether it is a movie, to look it up on other sites
    val names: List<String>,
    val movie: Boolean,
    // Day of the week of a new episode, null when the title is not airing weekly
    val releaseDay: DayOfWeek?,
    val year: Int?,
    val format: String,
    val genres: String,
    val synopsis: String,
    val nextEpisode: Int,
    // The user is in the middle of it, so the button continues where they stopped
    val continuing: Boolean,
    // Cover of AniList, the poster when no other source has one
    val coverUrl: String?,
    val artColors: List<Color>
)

// Latest activity of a friend, needs the account
data class StoryUi(
    val userId: Int,
    val name: String,
    val activity: String,
    val initial: String,
    val coverUrl: String?,
    val avatarUrl: String?,
    val coverColors: List<Color>,
    val avatarColors: List<Color>,
    val seen: Boolean
)

// Title shown as a poster
data class PosterUi(
    val anilistId: Int,
    val title: String,
    val details: String,
    // Id on MyAnimeList, where the advisory rating comes from, null for a manga, which has none there
    val malId: Int?,
    val artColors: List<Color>,
    val imageUrl: String?
)

// Episode that just aired
data class EpisodeUi(
    val anilistId: Int,
    val title: String,
    val details: String,
    val tag: String,
    val artColors: List<Color>,
    val imageUrl: String?
)

// What the Home is doing, loading with nothing to show, showing what came, or failed with nothing to show
enum class HomeStatus { Loading, Content, Error }

// The shelves of posters of the Home, the screen puts them in order
enum class HomeShelf(@StringRes val title: Int) {
    Watching(R.string.home_watching_title),
    Reading(R.string.home_reading_title),
    MissingSequels(R.string.home_missing_sequels),
    RecommendedAnime(R.string.home_recommended_anime),
    RecommendedManga(R.string.home_recommended_manga),
    Trending(R.string.home_trending_title),
    PlannedAnime(R.string.home_planned_anime),
    PlannedManga(R.string.home_planned_manga),
    NextSeason(R.string.home_next_season),
    NewAnime(R.string.home_new_anime),
    NewManga(R.string.home_new_manga)
}

// Where the cards get the advisory rating of their titles, which comes after the shelves, one card at a time
interface PosterRatings {
    // The rating already found, so a card that comes back to the screen shows it at once
    fun known(item: PosterUi): ContentRating?

    suspend fun load(item: PosterUi): ContentRating?
}

data class HomeUiState(
    val status: HomeStatus = HomeStatus.Loading,
    val heroes: List<HeroUi> = emptyList(),
    // Artwork of the featured titles already downloaded, by AniList id, a title that is not here is still loading
    val heroArts: Map<Int, HeroArt> = emptyMap(),
    // Size the posters of the featured titles are downloaded at, the page asks for the same one so it finds them in memory
    val posterSize: Size = Size.ORIGINAL,
    val stories: List<StoryUi> = emptyList(),
    val newEpisodes: List<EpisodeUi> = emptyList(),
    // The shelves of posters, each one shows up when it arrives, the ones that stay empty do not show
    val shelves: Map<HomeShelf, List<PosterUi>> = emptyMap()
)
