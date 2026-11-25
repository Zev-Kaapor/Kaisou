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
import com.kaisou.app.features.title.data.ThreadTitles
import com.kaisou.app.features.title.data.TitleRepository
import com.kaisou.app.features.title.data.TitleThread

// What the page of all the threads shows: the threads that came, whether there are more, and whether a page is coming
data class ThreadsUi(val threads: List<TitleThread> = emptyList(), val hasMore: Boolean = true, val loading: Boolean = false, val failed: Boolean = false)

// Asks for the threads about a title a page at a time, the one with the newest answer first, as AniList does
class ThreadsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(ThreadsUi())
    val state: StateFlow<ThreadsUi> = mutableState.asStateFlow()
    private var page = 0

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        loadMore()
    }

    // Asks for the next page when there is one and none is being asked
    fun loadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        mutableState.value = now.copy(loading = true, failed = false)
        viewModelScope.launch {
            val next = repository.threadsPage(id, page + 1)
            val before = mutableState.value
            mutableState.value = if (next == null) {
                before.copy(loading = false, failed = true)
            } else {
                page++
                before.copy(threads = (before.threads + next.threads).distinctBy { it.id }, hasMore = next.hasMore, loading = false)
            }

            // The titles show as they were written and come in the language of the app a moment after
            if (next != null) {
                val translated = ThreadTitles.translated(next.threads).associateBy { it.id }
                mutableState.update { state -> state.copy(threads = state.threads.map { translated[it.id] ?: it }) }
            }
        }
    }
}
