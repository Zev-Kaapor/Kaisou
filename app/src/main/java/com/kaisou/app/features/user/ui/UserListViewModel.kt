package com.kaisou.app.features.user.ui

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.navigation.UserListKindArg
import com.kaisou.app.core.navigation.UserListTabArg
import com.kaisou.app.core.navigation.UserNameArg
import com.kaisou.app.features.settings.data.AdultContent
import com.kaisou.app.features.title.data.ListChanges
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.user.data.ListFilter
import com.kaisou.app.features.user.data.ListPreference
import com.kaisou.app.features.user.data.ListSort
import com.kaisou.app.features.user.data.ListTabs
import com.kaisou.app.features.user.data.UserListEntry
import com.kaisou.app.features.user.data.UserListSection
import com.kaisou.app.features.user.data.UserListStore

// What the page of a list shows: the kind, the lists, the open tab, the words, what narrows it, and the order
data class UserListUi(
    val manga: Boolean = false,
    val sections: Load<List<UserListSection>> = Load.Loading,
    val tab: String = ListTabs.All,
    val search: String = "",
    val filter: ListFilter = ListFilter(),
    val mine: Set<Int>? = null,
    val sort: ListSort = ListSort.Updated,
    val reversed: Boolean = false
) {
    // The titles of a tab with the words, the filter and the order applied
    fun shown(tab: String): List<UserListEntry> {
        val all = (sections as? Load.Done)?.value?.let { ListTabs.entries(it, tab) }.orEmpty()
        val words = search.trim()
        return all.filter { entry ->
            (words.isEmpty() || entry.title.contains(words, ignoreCase = true) || entry.romaji.contains(words, ignoreCase = true)) &&
                filter.matches(entry, mine)
        }.sortedWith(sort.comparator(reversed))
    }
}

// The list of the user in the Library, or of the person whose page opened it on the tab touched, with tabs, words, genre and order
class UserListViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    // The person of the list: the one in the address, and the user when the Library opens it
    val owner: String = savedStateHandle.get<String>(UserNameArg) ?: AniListSession.viewer?.name.orEmpty()
    private val firstManga = savedStateHandle.get<String>(UserListKindArg) == MangaKind
    private val mutableState = MutableStateFlow(
        UserListUi(manga = firstManga, tab = savedStateHandle.get<String>(UserListTabArg) ?: ListTabs.All, sort = ListPreference.sort(firstManga), reversed = ListPreference.reversed(firstManga))
    )
    val state: StateFlow<UserListUi> = mutableState.asStateFlow()

    init {
        load()

        // A change of the list made anywhere in the app asks for it again with no placeholders, so the page keeps what it shows
        viewModelScope.launch { ListChanges.version.drop(1).collect { fetch(loading = false) } }

        // The choice about adult content changes which titles are on the lists, so they are asked for again
        viewModelScope.launch { AdultContent.mode.drop(1).collect { load() } }
    }

    // Asks for the list of the kind that is open, showing the placeholders while it comes
    fun load() = fetch(loading = true)

    // Asks for the list of the kind that is open, the one that is there staying until the new one comes
    private fun fetch(loading: Boolean) {
        val manga = mutableState.value.manga
        if (loading) mutableState.update { it.copy(sections = Load.Loading) }
        viewModelScope.launch {
            val viewer = AniListSession.viewer?.name
            val someoneElse = viewer != null && !viewer.equals(owner, ignoreCase = true)

            // A change by the user leaves the list of someone else alone, only what says which of its titles the user has
            if (loading || !someoneElse) {
                val found = if (owner.isBlank()) null else UserListStore.lists(getApplication(), owner, manga)
                if (!loading && found == null) return@launch

                // What came for the kind that was left is dropped
                mutableState.update { if (it.manga != manga) it else it.copy(sections = if (found == null) Load.Failed else Load.Done(found)) }
                if (found == null) return@launch
            }

            // On the list of someone else, the titles of the list of the user say which ones are on it too
            if (someoneElse && viewer != null) {
                val own = UserListStore.lists(getApplication(), viewer, manga)?.let { ListTabs.all(it).map { entry -> entry.mediaId }.toSet() }
                mutableState.update { if (it.manga != manga) it else it.copy(mine = own) }
            }
        }
    }

    // The other kind starts from its own order, with no tab, words or filter
    fun pickKind(manga: Boolean) {
        if (mutableState.value.manga == manga) return
        mutableState.value = UserListUi(manga = manga, sort = ListPreference.sort(manga), reversed = ListPreference.reversed(manga))
        load()
    }

    // Opens a tab
    fun pickTab(tab: String) = mutableState.update { it.copy(tab = tab) }

    // Sets the words looked for
    fun search(words: String) = mutableState.update { it.copy(search = words) }

    // Narrows the list with what the sheet of the filter chose
    fun pickFilter(filter: ListFilter) = mutableState.update { it.copy(filter = filter) }

    // The order and its way are kept on the phone for the next time, for the anime and the manga apart
    fun pickSort(sort: ListSort, reversed: Boolean) {
        ListPreference.setSort(mutableState.value.manga, sort, reversed)
        mutableState.update { it.copy(sort = sort, reversed = reversed) }
    }

    // Constants of the lists
    private companion object {
        // The word in the address for the list of manga
        const val MangaKind = "manga"
    }
}
