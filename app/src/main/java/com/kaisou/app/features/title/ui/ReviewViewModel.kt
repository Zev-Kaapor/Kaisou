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
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.navigation.ReviewIdArg
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.ReviewBody
import com.kaisou.app.features.title.data.MarkdownTranslator
import com.kaisou.app.features.title.data.ReviewVote
import com.kaisou.app.features.title.data.TitleRepository

// The review in the language of the app: its summary and its text
class ReviewTranslated(val summary: String, val text: String)

// What the page of a review shows: the review, whether it is shown translated, which is on until the user turns it off, and the translation
// when it is there
data class ReviewUi(
    val review: Load<ReviewBody> = Load.Loading,
    val translate: Boolean = true,
    val translated: ReviewTranslated? = null,
    val voting: Boolean = false
)

// Asks for the whole text of a review, the one the page is about, puts it in the language of the app, and gives the vote of the user
class ReviewViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[ReviewIdArg])
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(ReviewUi())
    val state: StateFlow<ReviewUi> = mutableState.asStateFlow()

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(review = Load.Loading) }
        viewModelScope.launch {
            val found = repository.review(id)
            mutableState.update { it.copy(review = if (found == null) Load.Failed else Load.Done(found)) }
            if (found != null) translateNow(found)
        }
    }

    // Turns the translation on or off. The text comes back in the language it was written when it is off
    fun toggleTranslate() {
        val on = !mutableState.value.translate
        mutableState.update { it.copy(translate = on) }
        val review = (mutableState.value.review as? Load.Done)?.value
        if (on && review != null && mutableState.value.translated == null) viewModelScope.launch { translateNow(review) }
    }

    // The summary and the text in the language of the app, when the translation is on
    private suspend fun translateNow(body: ReviewBody) {
        if (!mutableState.value.translate || mutableState.value.translated != null) return
        val language = AppLanguage.translationCode
        coroutineScope {
            // The summary goes with the text, and the text shows as each group of it comes back
            val summary = async { if (body.review.summary.isBlank()) "" else GoogleTranslator.translate(body.review.summary, language) ?: body.review.summary }
            val text = MarkdownTranslator.translate(body.text, language) { partial ->
                mutableState.update { it.copy(translated = ReviewTranslated(it.translated?.summary ?: body.review.summary, partial)) }
            }
            mutableState.update { it.copy(translated = ReviewTranslated(summary.await(), text)) }
        }
    }

    // The thumb of the review: it changes at once and goes back if AniList does not take it, and when it does the counts are the ones AniList
    // says. The same thumb again takes the vote back. A second touch while the first is going is left out
    fun vote(target: ReviewVote) {
        val now = mutableState.value
        val body = (now.review as? Load.Done)?.value ?: return
        if (now.voting) return
        val vote = if (body.vote == target) ReviewVote.None else target
        mutableState.update { it.copy(review = Load.Done(body.voted(vote)), voting = true) }
        viewModelScope.launch {
            val tally = repository.rateReview(id, vote)
            if (tally == null) {
                Toasts.show(getApplication<Application>().getString(R.string.title_review_vote_failed), isError = true)
            }
            mutableState.update { state ->
                val current = (state.review as? Load.Done)?.value ?: return@update state.copy(voting = false)
                state.copy(review = Load.Done(if (tally == null) body else current.copy(review = current.review.copy(helpful = tally.helpful, votes = tally.votes), vote = tally.vote)), voting = false)
            }
        }
    }
}

// The review with a vote of the user, counting it or not in what AniList counts
private fun ReviewBody.voted(vote: ReviewVote): ReviewBody {
    val helpful = review.helpful - (if (this.vote == ReviewVote.Up) 1 else 0) + (if (vote == ReviewVote.Up) 1 else 0)
    val votes = review.votes - (if (this.vote == ReviewVote.None) 0 else 1) + (if (vote == ReviewVote.None) 0 else 1)
    return copy(review = review.copy(helpful = helpful, votes = votes), vote = vote)
}
