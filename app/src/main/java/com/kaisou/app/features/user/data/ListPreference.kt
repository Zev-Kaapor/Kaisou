package com.kaisou.app.features.user.data

// Android framework
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Project resources
import com.kaisou.app.R

// How the titles of a list are ordered: last changed first, best rated first, by the name, newest first, each a way to compare two
enum class ListSort(@StringRes val label: Int, val comparator: Comparator<UserListEntry>) {
    Updated(R.string.user_sort_updated, compareByDescending { it.updatedAt }),
    Score(R.string.user_sort_score, compareByDescending<UserListEntry> { it.score }.thenByDescending { it.updatedAt }),
    Title(R.string.user_sort_title, compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }),
    Release(R.string.user_sort_release, compareByDescending<UserListEntry> { it.year ?: 0 }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title });

    // Finds a sort by its name
    companion object {
        // Gives the sort of a name, last changed when it is not known
        fun of(name: String?): ListSort = entries.firstOrNull { it.name == name } ?: Updated
    }
}

// How the user likes to see the lists, kept on the phone: grid or lines, and the order of the anime and of the manga apart
object ListPreference {
    // Name of the file and the keys of the grid and of the two orders
    private const val FileName = "kaisou_lists"
    private const val GridKey = "grid"
    private const val AnimeSortKey = "sort_anime"
    private const val MangaSortKey = "sort_manga"

    private lateinit var preferences: SharedPreferences
    private val mutableGrid = MutableStateFlow(true)
    val grid: StateFlow<Boolean> = mutableGrid.asStateFlow()

    // Opens the file of the choice and reads the grid
    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableGrid.value = preferences.getBoolean(GridKey, true)
    }

    // Changes grid or lines and keeps it
    fun setGrid(grid: Boolean) {
        mutableGrid.value = grid
        preferences.edit { putBoolean(GridKey, grid) }
    }

    // Gives the order kept for the anime or the manga
    fun sort(manga: Boolean): ListSort = ListSort.of(preferences.getString(if (manga) MangaSortKey else AnimeSortKey, null))

    // Changes the order of the anime or the manga and keeps it
    fun setSort(manga: Boolean, sort: ListSort) {
        preferences.edit { putString(if (manga) MangaSortKey else AnimeSortKey, sort.name) }
    }
}

// The states of AniList in the order the lists show them, with the lists the person made after them and all the titles at the end
object ListTabs {
    // The key of the tab with all the titles
    const val All = "*all"
    val StatusOrder = listOf("CURRENT", "REPEATING", "COMPLETED", "PAUSED", "DROPPED", "PLANNING")

    // The tabs of the lists of a person, the ones with nothing left out, and how many titles each has
    fun of(sections: List<UserListSection>): List<Pair<String, Int>> {
        val standard = StatusOrder.mapNotNull { status -> sections.firstOrNull { !it.custom && it.key == status }?.takeIf { it.entries.isNotEmpty() }?.let { status to it.entries.size } }
        val custom = sections.filter { it.custom && it.entries.isNotEmpty() }.map { it.key to it.entries.size }
        return standard + custom + (All to all(sections).size)
    }

    // All the titles of the lists, each once
    fun all(sections: List<UserListSection>): List<UserListEntry> = sections.filterNot { it.custom }.flatMap { it.entries }.distinctBy { it.mediaId }

    // The titles of a tab
    fun entries(sections: List<UserListSection>, tab: String): List<UserListEntry> =
        if (tab == All) all(sections) else sections.firstOrNull { it.key == tab }?.entries.orEmpty()
}
