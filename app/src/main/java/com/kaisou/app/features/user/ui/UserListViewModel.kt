package com.kaisou.app.features.user.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.user.data.ListPreference
import com.kaisou.app.features.user.data.ListSort
import com.kaisou.app.features.user.data.ListTabs
import com.kaisou.app.features.user.data.UserListEntry
import com.kaisou.app.features.user.data.UserListSection
import com.kaisou.app.features.user.data.UserListStore

// What the page of a list shows: the kind, the lists, the open tab, the words, the genre by its AniList name, and the order
data class UserListUi(
    val manga: Boolean = false,
    val sections: Load<List<UserListSection>> = Load.Loading,
    val tab: String = ListTabs.All,
    val search: String = "",
    val genre: String? = null,
    val sort: ListSort = ListSort.Updated
) {
    // The titles of a tab with the words, the filter and the order applied
    fun shown(tab: String): List<UserListEntry> {
        val all = (sections as? Load.Done)?.value?.let { ListTabs.entries(it, tab) }.orEmpty()
        val words = search.trim()
        return all.filter { entry ->
            (words.isEmpty() || entry.title.contains(words, ignoreCase = true) || entry.romaji.contains(words, ignoreCase = true)) &&
                (genre == null || genre in entry.genres)
        }.sortedWith(sort.comparator)
    }
}

// The list of the user, the Library, from what the profile already asked, with tabs, words, genre and order changing it locally
class UserListViewModel(application: Application) : AndroidViewModel(application) {
    private val name: String = AniListSession.viewer?.name.orEmpty()
    private val mutableState = MutableStateFlow(UserListUi(sort = ListPreference.sort(false)))
    val state: StateFlow<UserListUi> = mutableState.asStateFlow()

    init {
        load()
    }

    // Asks for the list of the kind that is open
    fun load() {
        val manga = mutableState.value.manga
        mutableState.update { it.copy(sections = Load.Loading) }
        viewModelScope.launch {
            val found = if (name.isBlank()) null else UserListStore.lists(getApplication(), name, manga)

            // What came for the kind that was left is dropped
            mutableState.update { if (it.manga != manga) it else it.copy(sections = if (found == null) Load.Failed else Load.Done(found)) }
        }
    }

    // The other kind starts from its own order, with no tab, words or genre
    fun pickKind(manga: Boolean) {
        if (mutableState.value.manga == manga) return
        mutableState.value = UserListUi(manga = manga, sort = ListPreference.sort(manga))
        load()
    }

    // Opens a tab
    fun pickTab(tab: String) = mutableState.update { it.copy(tab = tab) }

    // Sets the words looked for
    fun search(words: String) = mutableState.update { it.copy(search = words) }

    // Narrows the list to a genre
    fun pickGenre(genre: String?) = mutableState.update { it.copy(genre = genre) }

    // The order is kept on the phone for the next time, for the anime and the manga apart
    fun pickSort(sort: ListSort) {
        ListPreference.setSort(mutableState.value.manga, sort)
        mutableState.update { it.copy(sort = sort) }
    }
}
