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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.language.DubPreference
import com.kaisou.app.core.navigation.CastKindArg
import com.kaisou.app.core.navigation.CastStaff
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.title.data.TitlePerson
import com.kaisou.app.features.title.data.TitleRepository

// What the page of all the characters or all the staff of a title shows: the people that came, whether there are more to ask for, and
// whether the ones of a page are being asked for
data class CastUi(val people: List<TitlePerson> = emptyList(), val hasMore: Boolean = true, val loading: Boolean = false, val failed: Boolean = false)

// Asks for the characters or the staff of a title a page at a time, the next one when the list gets to its end
class CastViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    val staff: Boolean = savedStateHandle.get<String>(CastKindArg) == CastStaff
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(CastUi())
    val state: StateFlow<CastUi> = mutableState.asStateFlow()
    private var page = 0

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        loadMore()
        // The voices come again from the start when the user chooses another language
        if (!staff) viewModelScope.launch { DubPreference.language.drop(1).collect { restart() } }
    }

    private fun restart() {
        page = 0
        scroll.index = 0
        scroll.offset = 0
        mutableState.value = CastUi()
        loadMore()
    }

    fun loadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        mutableState.value = now.copy(loading = true, failed = false)
        viewModelScope.launch {
            val next = repository.castPage(id, staff, page + 1)
            val before = mutableState.value
            mutableState.value = if (next == null) {
                before.copy(loading = false, failed = true)
            } else {
                page++
                before.copy(people = (before.people + next.people).distinctBy { it.url }, hasMore = next.hasMore, loading = false)
            }
        }
    }
}
