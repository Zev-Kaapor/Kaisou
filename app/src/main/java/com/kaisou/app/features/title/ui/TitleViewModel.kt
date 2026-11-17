package com.kaisou.app.features.title.ui

// Android application and view model
import android.app.Application
import android.content.res.Configuration
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
import com.kaisou.app.features.title.data.TitleWatchOrder
import com.kaisou.app.features.title.ui.components.TitleTab

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
                draft.isPrivate, draft.hidden, draft.startedAt, draft.completedAt
            )
        )
        viewModelScope.launch {
            val application = getApplication<Application>()
            val saved = repository.saveEntry(before.anilistId, draft)
            if (saved != null) {
                setEntry(saved)
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
            // The order of the series is only for an anime, a manga has none to show
            if (title.manga) {
                mutableState.update { it.copy(watchOrder = Load.Done(TitleWatchOrder(emptyList(), 0))) }
            } else {
                loadWatchOrder(extras?.details?.relations.orEmpty())
            }
        }
    }

    fun retryWatchOrder() {
        val details = (mutableState.value.details as? Load.Done)?.value
        loadWatchOrder(details?.relations.orEmpty())
    }

    private fun loadWatchOrder(relations: List<TitleRelation>) {
        val title = mutableState.value.title ?: return
        mutableState.update { it.copy(watchOrder = Load.Loading) }
        viewModelScope.launch {
            val order = repository.watchOrder(title, relations)
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
        var season: Int? = null
        var covers = emptyList<MangaCover>()
        withTimeoutOrNull(ArtWaitMillis) {
            val context = getApplication<Application>()
            val loader = SingletonImageLoader.get(context)
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            coroutineScope {
                if (AppLanguage.needsTranslation) {
                    launch { synopsis = GoogleTranslator.translate(title.synopsis.take(TranslatedCharacters), AppLanguage.translationCode) }
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
                    season = SeasonNumber.resolve(SeasonNumber.byDate(title.startDate, found.seasonStarts), title.seasonMarker, title.hasPrequel)
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
        return HeroArt(posterUrl, backdropUrl, logoUrl, synopsis, rating, season)
    }

    private companion object {
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
