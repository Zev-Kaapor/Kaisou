package com.kaisou.app.features.profile.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Reactive flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.navigation.ProfileKindArg
import com.kaisou.app.core.navigation.ProfileTabArg
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.profile.data.MediaSort
import com.kaisou.app.features.profile.data.MediaTab
import com.kaisou.app.features.profile.data.ProfileKind
import com.kaisou.app.features.profile.data.ProfileMediaItem
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.profile.data.ProfileRepository

// What the page of all the titles of a person shows: the titles that came, the filters chosen, whether there are more and whether a page is
// being asked for
data class ProfileMediaUi(
    val items: List<ProfileMediaItem> = emptyList(),
    val tab: MediaTab = MediaTab.Anime,
    val onList: Boolean = false,
    val sort: MediaSort = MediaSort.Newest,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val failed: Boolean = false
)

// Asks for the titles of a character, a person of the staff or a studio a page at a time, again from the start when a filter changes
class ProfileMediaViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    val ref = ProfileRef(ProfileKind.of(savedStateHandle[ProfileKindArg]), checkNotNull(savedStateHandle[TitleIdArg]))
    private val repository = ProfileRepository(application)
    // The titles start from the newest, by year, in the tab the page was opened on
    private val mutableState = MutableStateFlow(ProfileMediaUi(tab = MediaTab.of(savedStateHandle[ProfileTabArg])))
    val state: StateFlow<ProfileMediaUi> = mutableState.asStateFlow()
    private var page = 0
    private var job: Job? = null

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        loadMore()
    }

    fun chooseTab(tab: MediaTab) = restart(mutableState.value.copy(tab = tab))

    fun chooseOnList(onList: Boolean) = restart(mutableState.value.copy(onList = onList))

    fun chooseSort(sort: MediaSort) = restart(mutableState.value.copy(sort = sort))

    // A filter changed: what came is thrown away and the list starts again
    private fun restart(next: ProfileMediaUi) {
        job?.cancel()
        page = 0
        scroll.index = 0
        scroll.offset = 0
        mutableState.value = next.copy(items = emptyList(), hasMore = true, loading = false, failed = false)
        loadMore()
    }

    fun loadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        mutableState.value = now.copy(loading = true, failed = false)
        job = viewModelScope.launch {
            val next = repository.mediaPage(ref, page + 1, now.tab, now.onList, now.sort)
            val before = mutableState.value
            mutableState.value = if (next == null) {
                before.copy(loading = false, failed = true)
            } else {
                page++
                before.copy(items = (before.items + next.items).distinctBy { it.key }, hasMore = next.hasMore, loading = false)
            }
        }
    }
}
