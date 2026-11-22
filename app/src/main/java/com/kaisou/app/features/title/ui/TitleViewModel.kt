package com.kaisou.app.features.title.ui

// Android application and view model
import android.app.Application
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Image loading
import coil3.SingletonImageLoader
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
// Coroutines and reactive flows
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
// Kotlin math
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.SeasonNumber
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.artwork.LogoCheck
import com.kaisou.app.core.artwork.artRequest
import com.kaisou.app.core.artwork.logoRequest
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.anilist.AniDescription
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.core.rating.ContentRating
import com.kaisou.app.core.rating.RatingQuery
import com.kaisou.app.core.rating.RatingRepository
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.features.home.data.HeroArt
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.title.data.ListDraft
import com.kaisou.app.features.title.data.ListEntry
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.MangaArtClient
import com.kaisou.app.features.title.data.MangaCover
import com.kaisou.app.features.title.data.NewsClient
import com.kaisou.app.features.title.data.ThemeClient
import com.kaisou.app.features.title.data.TitleRelation
import com.kaisou.app.features.title.data.TitleRepository
import com.kaisou.app.features.title.data.TitleStatus
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.data.TitleUiState
import com.kaisou.app.features.title.ui.components.TitleTab
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.features.title.data.ListChanges
import com.kaisou.app.features.title.data.TitleTrailer
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import com.kaisou.app.core.rating.AgeGroup

// Loads the title of the page as soon as it is opened and keeps it while the page is on the stack
class TitleViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(
        TitleUiState(
            posterSize = posterSize(application),
            backdropSize = backdropSize(application),
            logoSize = logoSize(application)
        )
    )
    val state: StateFlow<TitleUiState> = mutableState.asStateFlow()

    // Where the page was left, the tab that is open and the order of the episodes, which stay when the phone is turned
    val scroll = ScrollPosition()
    var tab by mutableStateOf(TitleTab.Details)
    var newestFirst by mutableStateOf(false)

    // Whether the favourite is going to AniList, so a second touch while the first is going is left out
    private var turningFavourite = false

    // When the title on the page was asked for
    private var askedAt = 0L

    init {
        load()
    }

    // The heart of the title: it changes at once and goes back if AniList does not take it. The island says what happened
    fun toggleFavourite() {
        val before = mutableState.value.title ?: return
        if (turningFavourite) return
        turningFavourite = true
        setFavourite(!before.isFavourite)
        viewModelScope.launch {
            val application = getApplication<Application>()
            if (repository.toggleFavourite(before.anilistId, before.manga)) {
                Toasts.show(application.getString(if (before.isFavourite) R.string.title_favourite_removed else R.string.title_favourite_added))
            } else {
                setFavourite(before.isFavourite)
                Toasts.show(application.getString(R.string.title_favourite_failed), isError = true)
            }
            turningFavourite = false
        }
    }

    // Whether the list is going to AniList, so a second touch while the first is going is left out
    private var savingEntry = false

    // Puts the title on the list or changes its entry, which shows at once and goes back if AniList does not take it. The island says what
    // happened
    fun saveEntry(draft: ListDraft) {
        val before = mutableState.value.title ?: return
        if (savingEntry) return
        savingEntry = true
        setEntry(
            ListEntry(
                before.entry?.id ?: 0, draft.status, draft.progress, draft.progressVolumes, draft.score, draft.repeat, draft.notes,
                draft.isPrivate, draft.hidden, draft.startedAt, draft.completedAt,
                draft.customLists ?: before.entry?.customLists.orEmpty(),
                draft.advancedScores?.let { scores -> mutableState.value.listOptions?.advancedScoring.orEmpty().zip(scores).toMap() } ?: before.entry?.advancedScores.orEmpty()
            )
        )
        viewModelScope.launch {
            val application = getApplication<Application>()
            val saved = repository.saveEntry(before.anilistId, draft)
            if (saved != null) {
                setEntry(saved)
                ListChanges.changed()
                Toasts.show(application.getString(R.string.title_list_saved))
            } else {
                setEntry(before.entry)
                Toasts.show(application.getString(R.string.title_list_failed), isError = true)
            }
            savingEntry = false
        }
    }

    // Takes the title off the list, which shows at once and goes back if AniList does not take it
    fun removeEntry() {
        val before = mutableState.value.title ?: return
        val entry = before.entry ?: return
        if (savingEntry) return
        savingEntry = true
        setEntry(null)
        viewModelScope.launch {
            val application = getApplication<Application>()
            if (repository.removeEntry(entry.id)) {
                ListChanges.changed()
                Toasts.show(application.getString(R.string.title_list_removed))
            } else {
                setEntry(entry)
                Toasts.show(application.getString(R.string.title_list_failed), isError = true)
            }
            savingEntry = false
        }
    }

    private fun setEntry(entry: ListEntry?) {
        mutableState.update { state ->
            state.copy(title = state.title?.copy(entry = entry, listStatus = entry?.status, nextUnit = entry?.nextUnit ?: 1))
        }
    }

    // Asks for the title again without showing it, to keep what AniList says on the page: the heart, the list and the score, when the user
    // comes back to the app after changing them there. The pictures and the synopsis stay. It waits while the heart is going, and a title
    // that was asked for a moment ago is not asked again, so going to another app and back does not use the requests AniList gives
    fun refresh() {
        if (mutableState.value.status != TitleStatus.Content || turningFavourite || savingEntry) return
        if (System.currentTimeMillis() - askedAt < RefreshMinMillis) return
        viewModelScope.launch {
            val fresh = repository.header(id) ?: return@launch
            askedAt = System.currentTimeMillis()
            if (!turningFavourite) mutableState.update { state -> if (state.status == TitleStatus.Content) state.copy(title = fresh) else state }
        }
    }

    private fun setFavourite(value: Boolean) {
        mutableState.update { state -> state.copy(title = state.title?.copy(isFavourite = value)) }
    }

    // Asks for the title, and then for its pictures, its synopsis and its rating. The page only shows when they are ready or the time
    // for them ended, so what shows does not change after it
    fun load() {
        mutableState.update { it.copy(status = TitleStatus.Loading) }
        viewModelScope.launch {
            val title = repository.header(id)
            if (title == null) {
                mutableState.update { it.copy(status = TitleStatus.Error) }
                return@launch
            }
            val art = loadArt(title)
            askedAt = System.currentTimeMillis()
            mutableState.update { it.copy(status = TitleStatus.Content, title = title, art = art) }
            loadExtras()
            loadListOptions(title.manga)
            if (!title.manga) loadThemes(title)
            loadNews(title)
        }
    }

    // What comes after the top: the details, what people recommend and the characters and the staff come together in one question, and
    // then the order of the series, which starts from what the details say is tied to the title. The page does not wait for them
    fun loadExtras() {
        val title = mutableState.value.title ?: return
        mutableState.update { it.copy(details = Load.Loading, recommendations = Load.Loading, people = Load.Loading) }
        viewModelScope.launch {
            val extras = repository.extras(title.anilistId)
            mutableState.update {
                if (extras == null) {
                    it.copy(details = Load.Failed, recommendations = Load.Failed, people = Load.Failed)
                } else {
                    it.copy(details = Load.Done(extras.details), recommendations = Load.Done(extras.recommendations), people = Load.Done(extras.people))
                }
            }
            if (extras != null && extras.details.trailer == null) loadFallbackTrailer(title)
            extras?.details?.ageGroup?.let { group -> applyMangaRating(group) }
            loadWatchOrder(extras?.details?.relations.orEmpty(), extras?.details?.malId)
        }
    }

    // The age of a manga, worked out from its tags, is its rating on the top of the page, as the one of an anime is. It comes with the details, after
    // the top, so it goes into it when it comes. It has the symbol of Brazil in Brazil and the square with the age in every other country
    private fun applyMangaRating(group: AgeGroup) {
        val rating = ContentRating(group, official = false, symbol = AppLanguage.region == BrazilRegion)
        mutableState.update { state -> state.art?.let { state.copy(art = it.withRating(rating)) } ?: state }
    }

    // The trailer TMDB has, for a title AniList has none for. It is asked once the details are there and show none, and it goes in them
    private fun loadFallbackTrailer(title: TitleUi) {
        if (title.manga) return
        viewModelScope.launch {
            val id = ArtworkRepository.Shared.trailer(title.anilistId, title.query, AppLanguage.code) ?: return@launch
            val trailer = TitleTrailer("https://www.youtube.com/watch?v=$id", "https://img.youtube.com/vi/$id/hqdefault.jpg", id)
            mutableState.update { state ->
                val details = (state.details as? Load.Done)?.value
                if (details != null && details.trailer == null) state.copy(details = Load.Done(details.copy(trailer = trailer))) else state
            }
        }
    }

    fun retryWatchOrder() {
        val details = (mutableState.value.details as? Load.Done)?.value
        loadWatchOrder(details?.relations.orEmpty(), details?.malId)
    }

    private fun loadWatchOrder(relations: List<TitleRelation>, malId: Int?) {
        val title = mutableState.value.title ?: return
        mutableState.update { it.copy(watchOrder = Load.Loading) }
        viewModelScope.launch {
            val order = repository.watchOrder(title.anilistId, title.manga, relations, malId)
            mutableState.update { it.copy(watchOrder = if (order == null) Load.Failed else Load.Done(order)) }
        }
    }

    // The episodes and the community are only asked when their tab is opened, and again when it is opened after a failure
    fun openTab(tab: TitleTab) {
        val title = mutableState.value.title ?: return
        when (tab) {
            TitleTab.Episodes -> if (!title.manga && mutableState.value.episodes.let { it is Load.Idle || it is Load.Failed }) loadEpisodes(title)
            TitleTab.Community -> if (mutableState.value.community.let { it is Load.Idle || it is Load.Failed }) loadCommunity(title)
            TitleTab.Details -> Unit
        }
    }

    fun retryEpisodes() {
        mutableState.value.title?.let(::loadEpisodes)
    }

    fun retryNews() {
        mutableState.value.title?.let(::loadNews)
    }

    fun retryCommunity() {
        mutableState.value.title?.let(::loadCommunity)
    }

    // How the user gives a score and the lists they made, from their account. The editor of the list uses them when it opens
    private fun loadListOptions(manga: Boolean) {
        viewModelScope.launch {
            val options = repository.listOptions(manga) ?: return@launch
            mutableState.update { it.copy(listOptions = options) }
        }
    }

    // The openings and the endings come from MyAnimeList by its id, so a title without one has none, and they are left out of the page when
    // they cannot be reached
    private fun loadThemes(title: TitleUi) {
        val malId = title.malId ?: return
        mutableState.update { it.copy(themes = Load.Loading) }
        viewModelScope.launch {
            val themes = ThemeClient.themes(malId)
            mutableState.update { it.copy(themes = if (themes == null) Load.Failed else Load.Done(themes)) }
        }
    }

    // The news come from Kuroiru by the id of MyAnimeList for an anime, and from MangaBaka by the one of AniList for a manga
    private fun loadNews(title: TitleUi) {
        mutableState.update { it.copy(news = Load.Loading) }
        viewModelScope.launch {
            val news = NewsClient.news(title.anilistId, title.malId, title.manga)
            mutableState.update { it.copy(news = if (news == null) Load.Failed else Load.Done(news)) }
        }
    }

    private fun loadEpisodes(title: TitleUi) {
        mutableState.update { it.copy(episodes = Load.Loading) }
        viewModelScope.launch {
            val episodes = repository.episodes(title)
            mutableState.update { it.copy(episodes = if (episodes == null) Load.Failed else Load.Done(episodes)) }
        }
    }

    private fun loadCommunity(title: TitleUi) {
        mutableState.update { it.copy(community = Load.Loading) }
        viewModelScope.launch {
            val community = repository.community(title.anilistId, title.manga)
            mutableState.update { it.copy(community = if (community == null) Load.Failed else Load.Done(community)) }
        }
    }

    // The same look for the pictures, the translation and the rating that the hero of the Home does, all at once. A manga has no art on
    // the other sites, which only know the anime, so it keeps the cover and the banner of AniList
    private suspend fun loadArt(title: TitleUi): HeroArt {
        var posterUrl: String? = null
        var backdropUrl: String? = null
        var logoUrl: String? = null
        var synopsis: String? = null
        var rating: ContentRating? = null
        var covers = emptyList<MangaCover>()
        // The season does not wait for the pictures, nor is it lost when one of the sites is slow: it goes on after the time of the pictures
        // ends and is put in the title when it comes
        val seasonJob = if (title.manga) null else viewModelScope.async {
            SeasonNumber.of(title.startDate, ArtworkRepository.Shared.seasonStarts(title.anilistId, title.query), title.seasonMarker, title.hasPrequel, title.isContinuation)
        }
        withTimeoutOrNull(ArtWaitMillis) {
            val context = getApplication<Application>()
            val loader = SingletonImageLoader.get(context)
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            coroutineScope {
                if (AppLanguage.needsTranslation) {
                    launch { synopsis = GoogleTranslator.translate(title.synopsis.take(TranslatedCharacters), AppLanguage.translationCode)?.let(AniDescription::tidy) }
                }
                if (title.manga) {
                    posterUrl = title.coverUrl
                    backdropUrl = title.bannerUrl
                    launch {
                        rating = RatingRepository.forCard(AppLanguage.region, RatingQuery(title.anilistId, title.malId, title.adult, true, title.query))
                    }
                    // The art of a manga is its covers on MangaDex, one for each volume, and the first volume stands for it. It is
                    // downloaded now, so it comes up whole, and the cover of AniList stays when there is none
                    launch {
                        val found = MangaArtClient.covers(title.anilistId, title.query.names, AppLanguage.code, AppLanguage.region).orEmpty()
                        covers = found
                        MangaArtClient.poster(found)?.original?.let { url ->
                            if (loader.execute(artRequest(context, url, mutableState.value.posterSize)) is SuccessResult) posterUrl = url
                        }
                    }
                    return@coroutineScope
                }
                launch {
                    val found = ArtworkRepository.Shared.art(title.anilistId, title.query, AppLanguage.code)
                    posterUrl = found.posterUrl
                    backdropUrl = found.backdropUrl
                    coroutineScope {
                        launch { rating = RatingRepository.find(AppLanguage.region, title.malId, title.adult, found.certifications) }
                        launch {
                            // The picture of the way the screen is in is downloaded now, so it comes up whole
                            if (landscape) {
                                backdropUrl = found.backdropUrl?.takeIf { loader.execute(artRequest(context, it, mutableState.value.backdropSize)) is SuccessResult }
                                if (backdropUrl == null) posterUrl = found.posterUrl?.takeIf { loader.execute(artRequest(context, it, mutableState.value.posterSize)) is SuccessResult }
                            } else {
                                posterUrl = found.posterUrl?.takeIf { loader.execute(artRequest(context, it, mutableState.value.posterSize)) is SuccessResult }
                            }
                        }
                        launch {
                            // The first logo that can be seen on the dark page, the black ones are turned down
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
        mutableState.update { it.copy(covers = covers) }
        val season = if (seasonJob?.isCompleted == true) seasonJob.await() else null
        if (seasonJob != null && !seasonJob.isCompleted) {
            viewModelScope.launch {
                val late = seasonJob.await() ?: return@launch
                // The page puts the pictures in when this ends, so the season is put in after them
                val shown = mutableState.first { it.art != null }
                mutableState.update { state -> state.art?.let { state.copy(art = it.withSeason(late)) } ?: shown }
            }
        }
        return HeroArt(posterUrl, backdropUrl, logoUrl, synopsis, rating, season)
    }

    private companion object {
        // The country whose symbols of rating are the official ones of the app
        const val BrazilRegion = "BR"

        // How long the page waits for its pictures before it shows with what it has
        const val ArtWaitMillis = 6_000L

        // The least time between two questions about the title that is on the page when the user comes back to the app
        const val RefreshMinMillis = 20_000L

        // How much of the synopsis is translated
        const val TranslatedCharacters = 2_000

        // The poster fills the width of the screen, standing up
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
