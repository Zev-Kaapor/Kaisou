package com.kaisou.app.features.home.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Image loading
import coil3.size.Size
// Coroutines and reactive flows
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Kotlin math
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.core.artwork.HeroArt
import com.kaisou.app.core.artwork.TitleQuery
import com.kaisou.app.core.artwork.loadHeroArt
import com.kaisou.app.features.home.data.HomeRepository
import com.kaisou.app.features.home.data.HomeShelf
import com.kaisou.app.features.home.data.PosterRatings
import com.kaisou.app.features.home.data.PosterUi
import com.kaisou.app.features.home.data.HomeStatus
import com.kaisou.app.features.home.data.HomeUiState

// Loads the Home as soon as the app opens and keeps it, so it is not asked again when the user scrolls or comes back to it
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeRepository(application)
    private val mutableState = MutableStateFlow(HomeUiState(posterSize = heroPosterSize(application)))
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    // Where the cards find the advisory rating of their titles
    val ratings: PosterRatings = repository

    // The request that is running, so a new one replaces it
    private var loadJob: Job? = null

    init {
        load()
    }

    // Asks for the Home. The titles and the shelves show as soon as AniList answers, with placeholders where the artwork
    // of the featured titles goes, and each one fills in as it is found and downloaded
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(status = HomeStatus.Loading, heroArts = emptyMap()) }
            val content = repository.load()
            if (content == null) {
                mutableState.update { it.copy(status = HomeStatus.Error) }
                return@launch
            }
            mutableState.update {
                it.copy(
                    status = HomeStatus.Content,
                    heroes = content.heroes,
                    stories = content.stories,
                    newEpisodes = content.newEpisodes,
                    shelves = content.shelves
                )
            }
            repository.prefetchRatings(content.shelves)

            // The shelves lower on the page come after, each one when it is ready
            val more = listOf<suspend () -> Map<HomeShelf, List<PosterUi>>>(
                { repository.recommended() },
                { repository.missingSequels() },
                { repository.discover() }
            )
            more.forEach { load ->
                launch {
                    val found = load()
                    if (found.isNotEmpty()) mutableState.update { it.copy(shelves = it.shelves + found) }
                    repository.prefetchRatings(found)
                }
            }

            content.heroes.forEach { hero ->
                launch {
                    val art = try {
                        loadHeroArt(
                            context = getApplication(),
                            anilistId = hero.anilistId,
                            malId = hero.malId,
                            posterSize = mutableState.value.posterSize,
                            query = TitleQuery(hero.names, hero.year, hero.movie),
                            anilistCover = hero.coverUrl,
                            anilistSynopsis = hero.synopsis
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        // Nothing was found, the page shows what AniList says
                        HeroArt(posterUrl = hero.coverUrl, logo = null, title = null, synopsis = null, rating = null)
                    }
                    mutableState.update { it.copy(heroArts = it.heroArts + (hero.anilistId to art)) }
                }
            }
        }
    }

    private companion object {
        // The poster of a featured title fills the width of the screen
        fun heroPosterSize(application: Application): Size {
            val width = application.resources.displayMetrics.widthPixels
            return Size(width, (width * HomeDimens.HeroAspect).roundToInt())
        }
    }
}
