package com.kaisou.app.features.home.ui

// Android application and view model
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import android.app.Application
import android.content.res.Configuration
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Image loading
import coil3.SingletonImageLoader
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import com.kaisou.app.core.designsystem.components.ScrollPosition
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
// Kotlin math and Java utilities
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.core.anilist.SeasonNumber
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.auth.AniListSession
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
import com.kaisou.app.features.home.data.HomeLayout
import com.kaisou.app.features.home.data.HomeParts
import com.kaisou.app.features.home.data.HomeRepository
import com.kaisou.app.features.settings.data.AdultContent
import com.kaisou.app.features.home.data.ComposerUi
import com.kaisou.app.features.home.data.HomeStatus
import com.kaisou.app.features.home.data.HomeUiState
import com.kaisou.app.features.home.data.RepliesUi
import com.kaisou.app.features.home.data.ReplyUi
import com.kaisou.app.features.home.data.StoryItemUi
import com.kaisou.app.features.home.data.StorySeen
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.features.title.data.ListDraft
import com.kaisou.app.features.title.data.ListOptions
import com.kaisou.app.features.title.data.TitleRepository
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.data.ListChanges
import com.kaisou.app.core.rating.MangaAgeRepository
import com.kaisou.app.features.home.data.ShelfUi

// Loads the Home as soon as it is asked for and keeps it, so it is not asked again when the user scrolls or comes back to it
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    // Where the Home was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    // The story that is open, kept here so it stays when the phone is turned
    var openStory by mutableStateOf<Int?>(null)
    var storyItem by mutableStateOf<Int?>(null)

    private val repository = HomeRepository(application)

    // The list of the user is the one of the page of the title, so the editor is the same
    private val titles = TitleRepository(application)
    private val artwork = ArtworkRepository.Shared
    private val storySeen = StorySeen(application)
    private val mutableState = MutableStateFlow(
        HomeUiState(
            posterSize = posterSize(application),
            backdropSize = backdropSize(application),
            logoSize = logoSize(application),
            seen = storySeen.ids()
        )
    )
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    // What the editor of the list needs for a title: the title with the user's entry and how the account scores and keeps lists
    class ListEditor(val title: TitleUi, val options: ListOptions?)

    // Loads what the editor of the list needs for a title
    suspend fun listEditor(id: Int): ListEditor? = coroutineScope {
        val options = async { titles.listOptions(manga = false) }
        val title = titles.header(id) ?: return@coroutineScope null
        ListEditor(title, options.await())
    }

    // Puts the title on the list or changes its entry, the island says what happened
    fun saveEntry(id: Int, draft: ListDraft) {
        viewModelScope.launch {
            val application = getApplication<Application>()
            if (titles.saveEntry(id, draft) != null) {
                ListChanges.changed()
                refreshIfStale(Long.MAX_VALUE)
                Toasts.show(application.getString(R.string.title_list_saved))
            } else {
                Toasts.show(application.getString(R.string.title_list_failed), isError = true)
            }
        }
    }

    // Takes the title off the list
    fun removeEntry(entryId: Int?) {
        if (entryId == null) return
        viewModelScope.launch {
            val application = getApplication<Application>()
            if (titles.removeEntry(entryId)) {
                ListChanges.changed()
                refreshIfStale(Long.MAX_VALUE)
                Toasts.show(application.getString(R.string.title_list_removed))
            } else {
                Toasts.show(application.getString(R.string.title_list_failed), isError = true)
            }
        }
    }

    // The request that is running, so a new one replaces it, the quiet one that looks for news, and when the Home was last asked for
    private var loadJob: Job? = null
    private var quietJob: Job? = null

    // What is known of what goes under the hero, which comes in four parts
    private var parts = HomeParts()

    // The activities whose like is going to AniList
    private val liking = mutableSetOf<Int>()
    private var loadedAt = 0L

    // When each part lower on the page was last asked for, so a quiet ask leaves out the ones that are still fresh
    private var belowAt = 0L
    private var recommendedAt = 0L
    private var sequelsAt = 0L
    private var listsAt = 0L

    // Whether the Home is the screen being used, so the lower parts are not asked while the user is somewhere else
    private val shown = MutableStateFlow(true)

    // Told by the screen when it comes to the front and when it leaves
    fun setShown(value: Boolean) {
        shown.value = value
    }

    // The last change of the list that the shelves were asked again for
    private var seenListVersion = ListChanges.version.value

    init {
        // TheTVDB is logged in to while the titles come, so the first title does not wait for it
        viewModelScope.launch { artwork.prewarm() }

        // The choice about adult content changes the hero, the stories and the shelves, so the Home is asked for again
        viewModelScope.launch { AdultContent.mode.drop(1).collect { load() } }

        // A list of states turned on in the settings is asked for at once, with no waiting for the Home to be asked again
        viewModelScope.launch {
            HomeLayout.state.drop(1).collect {
                val userId = AniListSession.viewer?.id ?: return@collect
                askLists(userId, force = true)
            }
        }
        load()
    }

    // The heart of an activity: it changes at once, goes back if AniList does not take it, and ends with the count AniList says
    fun toggleLike(activityId: Int) {
        if (!liking.add(activityId)) return
        val before = findItem(activityId)
        if (before == null) {
            liking.remove(activityId)
            return
        }
        editItem(activityId) { it.copy(isLiked = !it.isLiked, likeCount = (it.likeCount + if (it.isLiked) -1 else 1).coerceAtLeast(0)) }
        viewModelScope.launch {
            val result = repository.toggleLike(activityId)
            if (result == null) {
                editItem(activityId) { before }
            } else {
                editItem(activityId) { it.copy(isLiked = result.isLiked, likeCount = result.likeCount) }
            }
            liking.remove(activityId)
        }
    }

    // Opens the comments of an activity and asks for them
    fun openReplies(activityId: Int) {
        mutableState.update { it.copy(replies = RepliesUi(activityId)) }
        viewModelScope.launch {
            val items = repository.replies(activityId)
            mutableState.update { state ->
                val open = state.replies
                if (open?.activityId != activityId) state else state.copy(replies = open.copy(items = items ?: open.items, loading = false, failed = items == null))
            }
        }
    }

    // Closes the replies of an activity
    fun closeReplies() {
        mutableState.update { it.copy(replies = null) }
    }

    // Writes a comment on the open activity, or saves the new text of the one being edited, showing it when AniList takes it
    fun sendReply(text: String) {
        val open = mutableState.value.replies ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || open.sending) return
        mutableState.update { it.copy(replies = open.copy(sending = true, failed = false)) }
        viewModelScope.launch {
            val editing = open.editingId
            val reply = if (editing != null) repository.editReply(open.activityId, editing, trimmed) else repository.reply(open.activityId, trimmed)
            if (reply != null && editing == null) editItem(open.activityId) { it.copy(replyCount = it.replyCount + 1) }
            mutableState.update { state ->
                val current = state.replies
                when {
                    current?.activityId != open.activityId -> state
                    reply == null -> state.copy(replies = current.copy(sending = false, failed = true))
                    editing != null -> state.copy(
                        replies = current.copy(items = current.items.map { if (it.id == editing) reply else it }, sending = false, failed = false, editingId = null)
                    )
                    else -> state.copy(replies = current.copy(items = current.items + reply, sending = false, failed = false))
                }
            }
        }
    }

    // The heart of a comment, as the one of an activity: it changes at once and goes back if AniList does not take it
    fun toggleReplyLike(replyId: Int) {
        val before = mutableState.value.replies?.items?.firstOrNull { it.id == replyId } ?: return
        if (!liking.add(-replyId)) return
        editReply(replyId) { it.copy(isLiked = !it.isLiked, likeCount = (it.likeCount + if (it.isLiked) -1 else 1).coerceAtLeast(0)) }
        viewModelScope.launch {
            val result = repository.toggleReplyLike(replyId)
            if (result == null) {
                editReply(replyId) { before }
            } else {
                editReply(replyId) { it.copy(isLiked = result.isLiked, likeCount = result.likeCount) }
            }
            liking.remove(-replyId)
        }
    }

    // Deletes a comment of the user. It goes from the list at once, and if AniList does not delete it the comments are asked for again
    fun deleteReply(replyId: Int) {
        val open = mutableState.value.replies ?: return
        mutableState.update { state -> state.copy(replies = open.copy(items = open.items.filterNot { it.id == replyId }, editingId = null)) }
        editItem(open.activityId) { it.copy(replyCount = (it.replyCount - 1).coerceAtLeast(0)) }
        viewModelScope.launch {
            if (!repository.deleteReply(replyId)) openReplies(open.activityId)
        }
    }

    // A comment of the user is put in the field to be changed, and the change can be given up
    fun startEditingReply(replyId: Int) {
        mutableState.update { state -> state.copy(replies = state.replies?.copy(editingId = replyId, failed = false)) }
    }

    // Stops editing a reply
    fun cancelEditingReply() {
        mutableState.update { state -> state.copy(replies = state.replies?.copy(editingId = null, failed = false)) }
    }

    // Changes one reply of the open activity
    private fun editReply(id: Int, change: (ReplyUi) -> ReplyUi) {
        mutableState.update { state ->
            val open = state.replies ?: return@update state
            state.copy(replies = open.copy(items = open.items.map { if (it.id == id) change(it) else it }))
        }
    }

    // Finds a story item by its id, among the user's and the friends'
    private fun findItem(id: Int): StoryItemUi? {
        val state = mutableState.value
        return (state.ownStory + state.friends.flatMap { it.items }).firstOrNull { it.id == id }
    }

    // Changes one story item wherever it is
    private fun editItem(id: Int, change: (StoryItemUi) -> StoryItemUi) {
        mutableState.update { state ->
            state.copy(
                ownStory = state.ownStory.map { if (it.id == id) change(it) else it },
                friends = state.friends.map { friend -> friend.copy(items = friend.items.map { if (it.id == id) change(it) else it }) }
            )
        }
    }

    // An activity of a story was shown, so its piece of the ring turns gray
    fun markSeen(id: Int) {
        if (id in mutableState.value.seen) return
        val seen = storySeen.add(id)
        mutableState.update { it.copy(seen = seen) }
    }

    // The featured title that is on, kept here so it stays when the screen turns
    fun showHero(index: Int) {
        mutableState.update { it.copy(heroIndex = index) }
    }

    // Asks for the Home with placeholders until the titles come, a title showing only when its pictures are ready
    fun load() {
        start(refreshing = false)
    }

    // Asks again with the old answer kept aside, the Home on the screen staying until the new one comes or for good if it fails
    fun refresh() {
        val current = mutableState.value
        if (current.isRefreshing || current.status != HomeStatus.Content) return
        start(refreshing = true)
    }

    // Asks again for what is under the hero when the Home is older than the age given, quietly and not while another request runs
    fun refreshIfStale(minAgeMillis: Long) {
        val current = mutableState.value
        if (current.status != HomeStatus.Content || current.isRefreshing || loadJob?.isActive == true || quietJob?.isActive == true) return

        // A change of the list is asked for whatever the time since the last question
        val listChanged = ListChanges.version.value != seenListVersion
        if (!listChanged && SystemClock.elapsedRealtime() - loadedAt < minAgeMillis) return
        seenListVersion = ListChanges.version.value
        refreshUnder(force = listChanged)
    }

    // Asks quietly for what is under the hero, the lower parts only when older than their time or forced, as after a list change
    private fun refreshUnder(force: Boolean = false) {
        val userId = AniListSession.viewer?.id ?: return
        quietJob?.cancel()
        quietJob = viewModelScope.launch {
            repository.top(userId)?.let { top ->
                parts = parts.copy(top = top)
                loadedAt = SystemClock.elapsedRealtime()
                rebuild(userId)
            }
            askLower(userId, force)
        }
    }

    // Asks the lists of the user that were turned on, each one going into the page as it comes, the ones asked lately left alone
    private fun CoroutineScope.askLists(userId: Int, force: Boolean) {
        val wanted = HomeRepository.ListAsks.keys.filter { HomeLayout.state.value.shown(it.name) }
        if (wanted.isEmpty() || (!force && SystemClock.elapsedRealtime() - listsAt < HomeDimens.BelowRefreshMillis)) return
        launch {
            repository.lists(userId, wanted)?.let {
                listsAt = SystemClock.elapsedRealtime()
                parts = parts.copy(lists = it)
                rebuild(userId)
            }
        }
    }

    // Asks the lower parts at once, each going into the page as it comes, one asked or kept within its time left alone
    private fun CoroutineScope.askLower(userId: Int, force: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (force || now - belowAt >= HomeDimens.BelowRefreshMillis) launch {
            repository.below(userId, if (force) 0L else HomeDimens.BelowRefreshMillis)?.let {
                belowAt = SystemClock.elapsedRealtime()
                parts = parts.copy(below = it)
                rebuild(userId)
            }
        }
        if (force || now - recommendedAt >= HomeDimens.RecommendedRefreshMillis) launch {
            repository.recommended(userId, if (force) 0L else HomeDimens.RecommendedRefreshMillis)?.let {
                recommendedAt = SystemClock.elapsedRealtime()
                parts = parts.copy(recommended = it)
                rebuild(userId)
            }
        }
        askLists(userId, force)
        if (force || now - sequelsAt >= HomeDimens.SequelsRefreshMillis) launch {
            repository.sequels(userId, if (force) 0L else HomeDimens.SequelsRefreshMillis)?.let {
                sequelsAt = SystemClock.elapsedRealtime()
                parts = parts.copy(sequels = it)
                rebuild(userId)
            }
        }
    }

    // Puts what is known into the page, if the page is already on the screen. If it is not, it is put in when it shows
    private fun rebuild(userId: Int) {
        if (mutableState.value.status != HomeStatus.Content) return
        val built = repository.build(userId, parts) ?: return
        mutableState.update { it.copy(ownStory = built.ownStory, friends = built.friends, shelves = built.shelves) }
        prefetchMangaAges(built.shelves)
    }

    // Says all the manga of the shelves to the ones that work out their age, so they are asked together and the cards find it ready
    private fun prefetchMangaAges(shelves: List<ShelfUi>) {
        val ids = shelves.flatMap { shelf -> shelf.cards.filter { it.rating.manga }.map { it.rating.anilistId } }
        if (ids.isNotEmpty()) MangaAgeRepository.prefetch(ids)
    }

    // Deletes an activity of the user, which leaves the stories at once and comes back if AniList did not delete it
    fun deleteActivity(activityId: Int) {
        mutableState.update { state -> state.copy(ownStory = state.ownStory.filterNot { it.id == activityId }) }
        viewModelScope.launch {
            repository.deleteActivity(activityId)
            refreshUnder()
        }
    }

    // Opens the screen to write a status
    fun openComposer() {
        mutableState.update { it.copy(composer = ComposerUi()) }
    }

    // Closes the screen to write a status
    fun closeComposer() {
        mutableState.update { it.copy(composer = null) }
    }

    // Posts the status the user wrote, closing the sheet and asking for the stories again when AniList takes it
    fun postStatus(text: String) {
        val open = mutableState.value.composer ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || open.sending) return
        mutableState.update { it.copy(composer = open.copy(sending = true, failed = false)) }
        viewModelScope.launch {
            val posted = repository.postStatus(trimmed)
            mutableState.update { state ->
                if (state.composer == null) state else state.copy(composer = ComposerUi(sending = false, failed = !posted, sent = posted))
            }
            if (posted) refreshUnder()
        }
    }

    // Starts a request for the Home, cancelling the one that was running
    private fun start(refreshing: Boolean) {
        quietJob?.cancel()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update {
                if (refreshing) {
                    it.copy(isRefreshing = true)
                } else {
                    it.copy(
                        status = HomeStatus.Loading,
                        heroes = emptyList(),
                        art = emptyMap(),
                        heroIndex = 0,
                        ownStory = emptyList(),
                        friends = emptyList(),
                        shelves = emptyList()
                    )
                }
            }
            val userId = AniListSession.viewer?.id

            // What was kept of what is lower on the page is shown at once, and the new answers take its place as they come
            if (userId != null && !refreshing) parts = repository.kept(userId)

            // The top goes alone first, what the page waits for, and the lower parts after it, the fresh ones kept being left alone
            val top = async { userId?.let { repository.top(it) } }
            if (userId != null) launch {
                top.await()

                // With the kept parts on the screen the lower ones wait a moment, and are left for later if the user leaves meanwhile
                if (!refreshing && parts.below != null) {
                    val stayed = withTimeoutOrNull(HomeDimens.LowerDelayMillis) { shown.first { !it } } == null
                    if (!stayed) return@launch
                }
                askLower(userId, force = refreshing)
            }
            val heroes = repository.heroes(fresh = refreshing)
            if (heroes == null) {
                top.cancel()
                mutableState.update { if (refreshing) it.copy(isRefreshing = false) else it.copy(status = HomeStatus.Error) }
                return@launch
            }

            // The other titles of the hero go on while the page waits for the first one
            heroes.drop(1).forEach { hero -> launch { loadArt(hero) } }
            loadArt(heroes.first())
            parts = parts.copy(top = top.await())
            val built = userId?.let { repository.build(it, parts) }
            loadedAt = SystemClock.elapsedRealtime()
            mutableState.update {
                it.copy(
                    status = HomeStatus.Content,
                    isRefreshing = false,
                    heroes = heroes,
                    heroIndex = 0,
                    ownStory = built?.ownStory.orEmpty(),
                    friends = built?.friends.orEmpty(),
                    shelves = built?.shelves.orEmpty()
                )
            }
            built?.shelves?.let(::prefetchMangaAges)
        }
    }

    // Looks for the poster, logo, translated synopsis and rating at once, so the page gets them ready, leaving out what is late
    private suspend fun loadArt(hero: HeroUi) {
        var posterUrl: String? = null
        var backdropUrl: String? = null
        var logoUrl: String? = null
        var synopsis: String? = null
        var rating: ContentRating? = null

        // The season does not wait for the pictures, it goes on after their time ends and is put in the title when it comes
        val seasonJob = viewModelScope.async {
            SeasonNumber.of(hero.startDate, artwork.seasonStarts(hero.anilistId, hero.query), hero.seasonMarker, hero.hasPrequel, hero.isContinuation)
        }
        withTimeoutOrNull(ArtWaitMillis) {
            val context = getApplication<Application>()
            val loader = SingletonImageLoader.get(context)
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            coroutineScope {
                // AniList gives the synopsis in English, so it is translated from the start only when the app is not in English
                if (AppLanguage.needsTranslation) launch { synopsis = GoogleTranslator.translate(hero.synopsis.take(TranslatedCharacters), AppLanguage.translationCode) }
                launch {
                    val found = artwork.art(hero.anilistId, hero.query, AppLanguage.code)

                    // The picture for the way the screen is in is downloaded now, the other is only known until the screen turns
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
        val season = if (seasonJob.isCompleted) seasonJob.await() else null
        mutableState.update { it.copy(art = it.art + (hero.anilistId to HeroArt(posterUrl, backdropUrl, logoUrl, synopsis, rating, season))) }
        if (!seasonJob.isCompleted) {
            viewModelScope.launch {
                val late = seasonJob.await() ?: return@launch
                mutableState.update { state -> state.art[hero.anilistId]?.let { state.copy(art = state.art + (hero.anilistId to it.withSeason(late))) } ?: state }
            }
        }
    }

    // Constants of the view model
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
