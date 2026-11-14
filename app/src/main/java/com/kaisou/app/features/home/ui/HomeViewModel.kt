package com.kaisou.app.features.home.ui

// Android application and view model
import android.app.Application
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Image loading
import coil3.SingletonImageLoader
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
// Coroutines and reactive flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
// Kotlin math and Java utilities
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.core.anilist.SeasonNumber
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.artwork.LogoCheck
import com.kaisou.app.core.artwork.logoRequest
import com.kaisou.app.core.artwork.artRequest
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.rating.ContentRating
import com.kaisou.app.core.rating.RatingRepository
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.features.home.data.HeroArt
import com.kaisou.app.features.home.data.HeroUi
import com.kaisou.app.features.home.data.HomeRepository
import com.kaisou.app.features.home.data.HomeStatus
import com.kaisou.app.features.home.data.HomeUiState

// Loads the Home as soon as it is asked for and keeps it, so it is not asked again when the user scrolls or comes back to it
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeRepository(application)
    private val artwork = ArtworkRepository.Shared
    private val mutableState = MutableStateFlow(
        HomeUiState(posterSize = posterSize(application), backdropSize = backdropSize(application), logoSize = logoSize(application))
    )
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    // The request that is running, so a new one replaces it
    private var loadJob: Job? = null

    init {
        // TheTVDB is logged in to while the titles come, so the first title does not wait for it
        viewModelScope.launch { artwork.prewarm() }
        load()
    }

    // The featured title that is on, kept here so it stays when the screen turns
    fun showHero(index: Int) {
        mutableState.update { it.copy(heroIndex = index) }
    }

    // Asks for the Home, the placeholders show until the titles come. The poster and the logo of each title are looked for at
    // the same time as the others, and a title only shows when they are ready, so the picture never comes in low quality
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(status = HomeStatus.Loading, heroes = emptyList(), art = emptyMap(), heroIndex = 0) }
            val heroes = repository.heroes()
            if (heroes == null) {
                mutableState.update { it.copy(status = HomeStatus.Error) }
                return@launch
            }
            mutableState.update { it.copy(status = HomeStatus.Content, heroes = heroes) }
            heroes.forEach { hero -> launch { loadArt(hero) } }
        }
    }

    // Looks for the poster and the logo of a title and downloads them at the size of the page, translates its synopsis and finds its
    // rating, all at once. Only then does the page get them, so the pictures come up whole and from memory and the text does not change after it
    // shows. What is not found, or takes too long, is left out: the cover, the name in text and the synopsis as it came take its
    // place. What is ready when the time ends is kept
    private suspend fun loadArt(hero: HeroUi) {
        var posterUrl: String? = null
        var backdropUrl: String? = null
        var logoUrl: String? = null
        var synopsis: String? = null
        var rating: ContentRating? = null
        var season: Int? = null
        withTimeoutOrNull(ArtWaitMillis) {
            val context = getApplication<Application>()
            val loader = SingletonImageLoader.get(context)
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            coroutineScope {
                // AniList gives the synopsis in English, so there is nothing to translate when the app is in English. It goes on
                // from the start, it does not wait for the pictures to be found
                if (AppLanguage.needsTranslation) launch { synopsis = GoogleTranslator.translate(hero.synopsis.take(TranslatedCharacters), AppLanguage.translationCode) }
                launch {
                    val found = artwork.art(hero.anilistId, hero.query, AppLanguage.code)
                    season = SeasonNumber.resolve(SeasonNumber.byDate(hero.startDate, found.seasonStarts), hero.seasonMarker, hero.hasPrequel)
                    // The picture of the way the screen is in is downloaded now, so it comes up whole. The other one is only known,
                    // and comes when the screen turns
                    posterUrl = found.posterUrl
                    backdropUrl = found.backdropUrl
                    coroutineScope {
                        launch { rating = RatingRepository.find(AppLanguage.region, hero.malId, hero.adult, found.certifications) }
                        launch {
                            if (landscape) {
                                backdropUrl = found.backdropUrl?.takeIf { loader.execute(artRequest(context, it, mutableState.value.backdropSize)) is SuccessResult }
                                // With no wide picture the poster takes its place
                                if (backdropUrl == null) posterUrl = found.posterUrl?.takeIf { loader.execute(artRequest(context, it, mutableState.value.posterSize)) is SuccessResult }
                            } else {
                                posterUrl = found.posterUrl?.takeIf { loader.execute(artRequest(context, it, mutableState.value.posterSize)) is SuccessResult }
                            }
                        }
                        launch {
                            // The first one that can be seen on the dark page, the black ones are turned down
                            for (url in found.logos) {
                                val loaded = loader.execute(logoRequest(context, url, mutableState.value.logoSize))
                                if (loaded is SuccessResult && LogoCheck.isVisibleOnDark(loaded.image.toBitmap())) {
                                    logoUrl = url
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        mutableState.update { it.copy(art = it.art + (hero.anilistId to HeroArt(posterUrl, backdropUrl, logoUrl, synopsis, rating, season))) }
    }

    private companion object {
        // How long a title waits for its pictures before it shows with what it has
        const val ArtWaitMillis = 6_000L

        // How much of the synopsis is translated, the page shows only a few lines of it
        const val TranslatedCharacters = 600

        // The poster of a featured title fills the width of the screen, standing up
        fun posterSize(application: Application): Size {
            val metrics = application.resources.displayMetrics
            val width = minOf(metrics.widthPixels, metrics.heightPixels)
            return Size(width, (width * HomeDimens.HeroAspect).roundToInt())
        }

        // The backdrop fills the screen lying down, as wide as the long side and as tall as the short one
        fun backdropSize(application: Application): Size {
            val metrics = application.resources.displayMetrics
            return Size(maxOf(metrics.widthPixels, metrics.heightPixels), minOf(metrics.widthPixels, metrics.heightPixels))
        }

        // The logo fits in a share of the width and a height, the same box the title gives it
        fun logoSize(application: Application): Size {
            val metrics = application.resources.displayMetrics
            val width = minOf(metrics.widthPixels, metrics.heightPixels) * ComponentDimens.TitleLogoWidthFraction
            return Size(width.roundToInt(), (ComponentDimens.TitleLogoMaxHeight.value * metrics.density).roundToInt())
        }
    }
}
