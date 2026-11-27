package com.kaisou.app.features.user.data

// Android framework
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// How the user likes to see the lists, kept on the phone: grid or lines, and the order and its way for the anime and the manga apart
object ListPreference {
    // Name of the file and the keys of the grid, of the two orders and of the way each goes
    private const val FileName = "kaisou_lists"
    private const val GridKey = "grid"
    private const val AnimeSortKey = "sort_anime"
    private const val MangaSortKey = "sort_manga"
    private const val AnimeReversedKey = "reversed_anime"
    private const val MangaReversedKey = "reversed_manga"

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

    // Tells whether the person turned the order of the anime or of the manga round
    fun reversed(manga: Boolean): Boolean = preferences.getBoolean(if (manga) MangaReversedKey else AnimeReversedKey, false)

    // Changes the order of the anime or the manga and the way it goes, and keeps them
    fun setSort(manga: Boolean, sort: ListSort, reversed: Boolean) {
        preferences.edit {
            putString(if (manga) MangaSortKey else AnimeSortKey, sort.name)
            putBoolean(if (manga) MangaReversedKey else AnimeReversedKey, reversed)
        }
    }
}

// The states of AniList in the order the lists show them, with the lists the person made after them and all the titles at the end
object ListTabs {
    // The key of the tab with all the titles
    const val All = "*all"
    val StatusOrder = listOf("CURRENT", "REPEATING", "COMPLETED", "PAUSED", "DROPPED", "PLANNING")
    val PageOrder = listOf("CURRENT", "REPEATING", "PAUSED", "DROPPED", "COMPLETED", "PLANNING")

    // The tabs of a list in the order given, the ones with nothing left out, the lists of their own, and all the titles at the end
    fun of(sections: List<UserListSection>, order: List<String> = StatusOrder): List<Pair<String, Int>> {
        val standard = order.mapNotNull { status -> sections.firstOrNull { !it.custom && it.key == status }?.takeIf { it.entries.isNotEmpty() }?.let { status to it.entries.size } }
        val custom = sections.filter { it.custom && it.entries.isNotEmpty() }.map { it.key to it.entries.size }
        return standard + custom + (All to all(sections).size)
    }

    // The tabs of the page of a list, from what is being watched to what is planned
    fun pageTabs(sections: List<UserListSection>): List<Pair<String, Int>> = of(sections, PageOrder)

    // All the titles of the lists, each once
    fun all(sections: List<UserListSection>): List<UserListEntry> = sections.filterNot { it.custom }.flatMap { it.entries }.distinctBy { it.mediaId }

    // The titles of a tab
    fun entries(sections: List<UserListSection>, tab: String): List<UserListEntry> =
        if (tab == All) all(sections) else sections.firstOrNull { it.key == tab }?.entries.orEmpty()
}
