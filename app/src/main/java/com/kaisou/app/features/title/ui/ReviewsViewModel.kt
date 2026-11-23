package com.kaisou.app.features.title.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.title.data.ReviewSummaries
import com.kaisou.app.features.title.data.TitleRepository
import com.kaisou.app.features.title.data.TitleReview

// What the page of all the reviews of a title shows: the reviews that came, whether there are more to ask for, and whether a page is
// being asked for
data class ReviewsUi(val reviews: List<TitleReview> = emptyList(), val hasMore: Boolean = true, val loading: Boolean = false, val failed: Boolean = false)

// Asks for the reviews of a title a page at a time, the most useful first as AniList does, the next page when the list gets to its end
class ReviewsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(ReviewsUi())
    val state: StateFlow<ReviewsUi> = mutableState.asStateFlow()
    private var page = 0

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        loadMore()
    }

    fun loadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        mutableState.value = now.copy(loading = true, failed = false)
        viewModelScope.launch {
            val next = repository.reviewsPage(id, page + 1)
            val before = mutableState.value
            mutableState.value = if (next == null) {
                before.copy(loading = false, failed = true)
            } else {
                page++
                before.copy(reviews = (before.reviews + next.reviews).distinctBy { it.id }, hasMore = next.hasMore, loading = false)
            }
            // The summaries show as they were written and come in the language of the app a moment after
            if (next != null) {
                val translated = ReviewSummaries.translated(next.reviews).associateBy { it.id }
                mutableState.update { state -> state.copy(reviews = state.reviews.map { translated[it.id] ?: it }) }
            }
        }
    }
}
