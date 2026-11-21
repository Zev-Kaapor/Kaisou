package com.kaisou.app.features.home.ui

// Android application and view model
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
import com.kaisou.app.features.home.data.HomeParts
import com.kaisou.app.features.home.data.HomeRepository
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

// Loads the Home as soon as it is asked for and keeps it, so it is not asked again when the user scrolls or comes back to it
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    // Where the Home was left, which stays when the phone is turned
    val scroll = ScrollPosition()

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

    // What the editor of the list needs for a title: the title with the entry of the user and how the account gives scores and keeps lists
    class ListEditor(val title: TitleUi, val options: ListOptions?)

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

    // The request that is running, so a new one replaces it, the one that looks for news on its own, and when the Home was last asked for
    private var loadJob: Job? = null
    private var quietJob: Job? = null
    // What is known of what goes under the hero, which comes in four parts
    private var parts = HomeParts()
    // The activities whose like is going to AniList
    private val liking = mutableSetOf<Int>()
    private var loadedAt = 0L
    // The last change of the list that the shelves were asked again for
    private var seenListVersion = ListChanges.version.value

    init {
        // TheTVDB is logged in to while the titles come, so the first title does not wait for it
        viewModelScope.launch { artwork.prewarm() }
        load()
    }

    // The heart of an activity: it changes at once and goes back if AniList does not take it, and when it does the count is the one
    // AniList says. A second touch while the first is going is left out
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

    fun closeReplies() {
        mutableState.update { it.copy(replies = null) }
    }

    // Writes a comment on the activity that is open, or saves the new text of the one that is being changed. It shows in the list when
    // AniList takes it
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

    fun cancelEditingReply() {
        mutableState.update { state -> state.copy(replies = state.replies?.copy(editingId = null, failed = false)) }
    }

    private fun editReply(id: Int, change: (ReplyUi) -> ReplyUi) {
        mutableState.update { state ->
            val open = state.replies ?: return@update state
            state.copy(replies = open.copy(items = open.items.map { if (it.id == id) change(it) else it }))
        }
    }

    private fun findItem(id: Int): StoryItemUi? {
        val state = mutableState.value
        return (state.ownStory + state.friends.flatMap { it.items }).firstOrNull { it.id == id }
    }

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

    // Asks for the Home, the placeholders show until the titles come. The poster and the logo of each title are looked for at
    // the same time as the others, and a title only shows when they are ready, so the picture never comes in low quality
    fun load() {
        start(refreshing = false)
    }

    // Asks again with what was asked before kept aside, since the point is to see what changed. The Home on the screen stays until the
    // new one comes, and if it does not come it stays
    fun refresh() {
        val current = mutableState.value
        if (current.isRefreshing || current.status != HomeStatus.Content) return
        start(refreshing = true)
    }

    // What is under the hero is asked for again by itself when the Home is older than the age given, so the news of the friends come
    // without pulling. The hero stays, it only changes when asked, and nothing shows that it is asking. It does nothing while
    // another request is running
    fun refreshIfStale(minAgeMillis: Long) {
        val current = mutableState.value
        if (current.status != HomeStatus.Content || current.isRefreshing || loadJob?.isActive == true || quietJob?.isActive == true) return
        // A change of the list is asked for whatever the time since the last question
        val listChanged = ListChanges.version.value != seenListVersion
        if (!listChanged && SystemClock.elapsedRealtime() - loadedAt < minAgeMillis) return
        seenListVersion = ListChanges.version.value
        refreshUnder()
    }

    // Asks for what is under the hero again, quietly, and swaps each part in when it comes
    private fun refreshUnder() {
        val userId = AniListSession.viewer?.id ?: return
        quietJob?.cancel()
        quietJob = viewModelScope.launch {
            repository.top(userId)?.let { top ->
                parts = parts.copy(top = top)
                loadedAt = SystemClock.elapsedRealtime()
                rebuild(userId)
            }
            askLower(userId)
        }
    }

    // The three lower parts of what is under the hero are asked for at the same time and each one goes into the page as it comes, none of
    // them holds the page
    private fun CoroutineScope.askLower(userId: Int) {
        launch { repository.below(userId)?.let { parts = parts.copy(below = it); rebuild(userId) } }
        launch { repository.recommended(userId)?.let { parts = parts.copy(recommended = it); rebuild(userId) } }
        launch { repository.sequels(userId)?.let { parts = parts.copy(sequels = it); rebuild(userId) } }
    }

    // Puts what is known into the page, if the page is already on the screen. If it is not, it is put in when it shows
    private fun rebuild(userId: Int) {
        if (mutableState.value.status != HomeStatus.Content) return
        val built = repository.build(userId, parts) ?: return
        mutableState.update { it.copy(ownStory = built.ownStory, friends = built.friends, shelves = built.shelves) }
    }

    // Deletes an activity of the user. It goes from the stories at once, and when AniList has answered they are asked for again, so one
    // that was not deleted comes back
    fun deleteActivity(activityId: Int) {
        mutableState.update { state -> state.copy(ownStory = state.ownStory.filterNot { it.id == activityId }) }
        viewModelScope.launch {
            repository.deleteActivity(activityId)
            refreshUnder()
        }
    }

    fun openComposer() {
        mutableState.update { it.copy(composer = ComposerUi()) }
    }

    fun closeComposer() {
        mutableState.update { it.copy(composer = null) }
    }

    // Posts the status the user wrote. When AniList takes it the sheet is told to close, and the stories are asked for again so the
    // new one shows
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
            // The top of the shelves is what the page waits for along with the hero, so it goes alone first, with nothing in its way in the queue
            // of AniList. What is lower on the page is asked for when it has answered, and does not hold the page: each part comes into it
            // when it is ready
            val top = async { userId?.let { repository.top(it) } }
            if (userId != null) launch {
                top.await()
                askLower(userId)
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
        // The season does not wait for the pictures, nor is it lost when one of the sites is slow: it goes on after the time of the pictures
        // ends and is put in the title when it comes
        val seasonJob = viewModelScope.async {
            SeasonNumber.of(hero.startDate, artwork.seasonStarts(hero.anilistId, hero.query), hero.seasonMarker, hero.hasPrequel, hero.isContinuation)
        }
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
        val season = if (seasonJob.isCompleted) seasonJob.await() else null
        mutableState.update { it.copy(art = it.art + (hero.anilistId to HeroArt(posterUrl, backdropUrl, logoUrl, synopsis, rating, season))) }
        if (!seasonJob.isCompleted) {
            viewModelScope.launch {
                val late = seasonJob.await() ?: return@launch
                mutableState.update { state -> state.art[hero.anilistId]?.let { state.copy(art = state.art + (hero.anilistId to it.withSeason(late))) } ?: state }
            }
        }
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
