package com.kaisou.app.features.title.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Reactive flows
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.navigation.ThreadIdArg
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.features.home.data.ComposerUi
import com.kaisou.app.features.title.data.ForumLinks
import com.kaisou.app.features.title.data.LinkedTitle
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.MarkdownTranslator
import com.kaisou.app.features.title.data.ThreadBody
import com.kaisou.app.features.title.data.ThreadComment
import com.kaisou.app.features.title.data.TitleRepository

// The thread in the language of the app: its title and its text
class ThreadTranslated(val title: String, val text: String)

// The screen of writing a comment: the comment it answers, none for one on the thread itself, and how it is going
class ThreadComposer(val parentId: Int?, val state: ComposerUi = ComposerUi())

// What the page of a thread shows: the thread, its translation, the comments, more to ask, linked titles and the writing screen
data class ThreadUi(
    val thread: Load<ThreadBody> = Load.Loading,
    val translate: Boolean = true,
    val translated: ThreadTranslated? = null,
    val comments: List<ThreadComment> = emptyList(),
    val hasMore: Boolean = true,
    val loadingComments: Boolean = false,
    val commentsFailed: Boolean = false,
    val cards: Map<Int, LinkedTitle> = emptyMap(),
    val composer: ThreadComposer? = null,
    // The texts of the comments in the language of the app, by comment, as they come back
    val commentTexts: Map<Int, String> = emptyMap()
)

// Asks for a thread with its text, puts it in the language of the app and asks for the comments a page at a time
class ThreadViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[ThreadIdArg])
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(ThreadUi())
    val state: StateFlow<ThreadUi> = mutableState.asStateFlow()
    private var page = 0

    // The hearts and the bell that are going to AniList, so a second touch while the first is going is left out
    private val liking = HashSet<Int>()
    private var likingThread = false
    private var following = false

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        load()
    }

    // Asks for the thread and its comments
    fun load() {
        mutableState.update { it.copy(thread = Load.Loading) }
        viewModelScope.launch {
            val found = repository.thread(id)
            mutableState.update { it.copy(thread = if (found == null) Load.Failed else Load.Done(found)) }
            if (found != null) {
                launch { loadCards(found.text) }
                translateNow(found)
            }
        }
        loadMoreComments()
    }

    // The next page of the comments
    fun loadMoreComments() {
        val now = mutableState.value
        if (now.loadingComments || !now.hasMore) return
        mutableState.update { it.copy(loadingComments = true, commentsFailed = false) }
        viewModelScope.launch {
            val next = repository.threadComments(id, page + 1)
            mutableState.update { state ->
                if (next == null) {
                    state.copy(loadingComments = false, commentsFailed = true)
                } else {
                    page++
                    state.copy(comments = (state.comments + next.comments).distinctBy { it.id }, hasMore = next.hasMore, loadingComments = false)
                }
            }
            if (next != null) {
                launch { loadCards(next.comments.flatMap(::allTexts).joinToString("\n")) }
                translateComments(next.comments)
            }
        }
    }

    // The comments with their answers in one list
    private fun flat(comment: ThreadComment): List<ThreadComment> = listOf(comment) + comment.replies.flatMap(::flat)

    // The texts of the comments that were not put in the language of the app yet, all together, shown as each group of them comes back
    private suspend fun translateComments(comments: List<ThreadComment>) {
        if (!mutableState.value.translate) return
        val pending = comments.flatMap(::flat).filter { it.text.isNotBlank() && it.id !in mutableState.value.commentTexts }
        if (pending.isEmpty()) return
        val done = MarkdownTranslator.translateAll(pending.map { it.text }, AppLanguage.translationCode) { partial ->
            mutableState.update { it.copy(commentTexts = it.commentTexts + pending.map { comment -> comment.id }.zip(partial)) }
        }
        mutableState.update { it.copy(commentTexts = it.commentTexts + pending.map { comment -> comment.id }.zip(done)) }
    }

    // The texts of a comment and of its answers
    private fun allTexts(comment: ThreadComment): List<String> = listOf(comment.text) + comment.replies.flatMap(::allTexts)

    // The titles that a text links to, which are not known yet, asked together to be shown as cards
    private suspend fun loadCards(text: String) {
        val missing = ForumLinks.ids(text).filter { it !in mutableState.value.cards }
        if (missing.isEmpty()) return
        val found = repository.linkedTitles(missing)
        if (found.isNotEmpty()) mutableState.update { it.copy(cards = it.cards + found) }
    }

    // Turns the translation on or off. The text comes back in the language it was written when it is off
    fun toggleTranslate() {
        val on = !mutableState.value.translate
        mutableState.update { it.copy(translate = on) }
        val thread = (mutableState.value.thread as? Load.Done)?.value
        if (on && thread != null && mutableState.value.translated == null) viewModelScope.launch { translateNow(thread) }
        if (on) viewModelScope.launch { translateComments(mutableState.value.comments) }
    }

    // The title and the text in the language of the app, when the translation is on. The text shows as each group of it comes back
    private suspend fun translateNow(body: ThreadBody) {
        if (!mutableState.value.translate || mutableState.value.translated != null) return
        val language = AppLanguage.translationCode
        coroutineScope {
            val title = async { GoogleTranslator.translate(body.thread.title, language) ?: body.thread.title }
            val text = MarkdownTranslator.translate(body.text, language) { partial ->
                mutableState.update { it.copy(translated = ThreadTranslated(it.translated?.title ?: body.thread.title, partial)) }
            }
            mutableState.update { it.copy(translated = ThreadTranslated(title.await(), text)) }
        }
    }

    // The heart of the thread changes at once and goes back if AniList does not take it, ending with the count AniList says
    fun toggleLike() {
        val body = (mutableState.value.thread as? Load.Done)?.value ?: return
        if (likingThread) return
        likingThread = true
        mutableState.update { it.copy(thread = Load.Done(body.withLike(!body.liked, body.thread.likes + if (body.liked) -1 else 1))) }
        viewModelScope.launch {
            val result = repository.toggleThreadLike(id)
            if (result == null) failed(R.string.title_thread_like_failed)
            mutableState.update { state ->
                val current = (state.thread as? Load.Done)?.value ?: return@update state
                state.copy(thread = Load.Done(if (result == null) body else current.withLike(result.isLiked, result.likeCount)))
            }
            likingThread = false
        }
    }

    // The heart of a comment, the same way
    fun toggleCommentLike(commentId: Int) {
        if (!liking.add(commentId)) return
        mutableState.update { state -> state.copy(comments = edit(state.comments, commentId) { it.copy(liked = !it.liked, likes = (it.likes + if (it.liked) -1 else 1).coerceAtLeast(0)) }) }
        viewModelScope.launch {
            val result = repository.toggleCommentLike(commentId)
            if (result == null) failed(R.string.title_thread_like_failed)
            mutableState.update { state ->
                state.copy(comments = edit(state.comments, commentId) {
                    if (result == null) it.copy(liked = !it.liked, likes = (it.likes + if (it.liked) -1 else 1).coerceAtLeast(0)) else it.copy(liked = result.isLiked, likes = result.likeCount)
                })
            }
            liking.remove(commentId)
        }
    }

    // The bell of the thread: follows it or stops following it, at once, and goes back if AniList does not take it
    fun toggleFollow() {
        val body = (mutableState.value.thread as? Load.Done)?.value ?: return
        if (following) return
        following = true
        mutableState.update { it.copy(thread = Load.Done(body.copy(subscribed = !body.subscribed))) }
        viewModelScope.launch {
            val result = repository.subscribeThread(id, !body.subscribed)
            if (result == null) {
                failed(R.string.title_thread_follow_failed)
            } else {
                // The island says what the bell did
                val application = getApplication<Application>()
                Toasts.show(
                    message = application.getString(if (result) R.string.thread_followed else R.string.thread_unfollowed),
                    icon = if (result) KaisouIcons.BellRinging else KaisouIcons.Bell
                )
            }
            mutableState.update { state ->
                val current = (state.thread as? Load.Done)?.value ?: return@update state
                state.copy(thread = Load.Done(current.copy(subscribed = result ?: body.subscribed)))
            }
            following = false
        }
    }

    // Opens the screen of writing, for a comment on the thread or for an answer to the comment given
    fun openComposer(parentId: Int?) {
        mutableState.update { it.copy(composer = ThreadComposer(parentId)) }
    }

    // Closes the screen of writing
    fun closeComposer() {
        mutableState.update { it.copy(composer = null) }
    }

    // Posts what the user wrote, placing it at the end of the comments or of the answers of the comment it answers
    fun postComment(text: String) {
        val open = mutableState.value.composer ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || open.state.sending) return
        mutableState.update { it.copy(composer = ThreadComposer(open.parentId, ComposerUi(sending = true))) }
        viewModelScope.launch {
            val saved = repository.saveComment(id, open.parentId, trimmed)
            mutableState.update { state ->
                if (state.composer == null) return@update state
                if (saved == null) return@update state.copy(composer = ThreadComposer(open.parentId, ComposerUi(failed = true)))
                val comments = if (open.parentId == null) state.comments + saved else edit(state.comments, open.parentId) { it.copy(replies = it.replies + saved) }
                state.copy(comments = comments, composer = ThreadComposer(open.parentId, ComposerUi(sent = true)))
            }
        }
    }

    // Shows a failure notice
    private fun failed(message: Int) {
        Toasts.show(getApplication<Application>().getString(message), isError = true)
    }

    // The comments with the one that has the id changed, wherever it is among the answers
    private fun edit(comments: List<ThreadComment>, commentId: Int, change: (ThreadComment) -> ThreadComment): List<ThreadComment> =
        comments.map { comment ->
            if (comment.id == commentId) change(comment) else comment.copy(replies = edit(comment.replies, commentId, change))
        }
}

// The thread with the heart of the user and how many gave one
private fun ThreadBody.withLike(liked: Boolean, likes: Int): ThreadBody = copy(thread = thread.copy(likes = likes.coerceAtLeast(0)), liked = liked)
