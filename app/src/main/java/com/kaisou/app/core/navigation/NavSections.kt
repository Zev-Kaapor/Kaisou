package com.kaisou.app.core.navigation

// Coroutines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// UI styling and utilities
import androidx.compose.ui.graphics.vector.ImageVector

// The sections a screen asks the bar to show in place of the main destinations: their names, their icons and the one that is open
data class NavSections(val labels: List<String>, val icons: List<ImageVector>, val selected: Int)

// Where the screens put their sections for the bar. Each screen keeps its own with what to do when one is chosen, and the one that came
// last is the one the bar shows, so when a title opens another the second one is shown while the first is still there and when it goes the
// first one is again, with its own choice. A screen takes away only what it put
object NavSectionsHost {
    private class Entry(val owner: Any, var sections: NavSections, var onSelect: (Int) -> Unit)

    private val entries = mutableListOf<Entry>()
    private val mutableSections = MutableStateFlow<NavSections?>(null)
    val sections: StateFlow<NavSections?> = mutableSections.asStateFlow()

    // Puts or updates the sections of a screen. A screen that is already there keeps its place, so one that is going out and draws again does
    // not take the bar from the one that is coming
    fun show(owner: Any, sections: NavSections, onSelect: (Int) -> Unit) {
        val entry = entries.firstOrNull { it.owner === owner } ?: Entry(owner, sections, onSelect).also { entries += it }
        entry.sections = sections
        entry.onSelect = onSelect
        publish()
    }

    fun clear(owner: Any) {
        entries.removeAll { it.owner === owner }
        publish()
    }

    // A choice of the bar goes to the screen that is shown
    fun select(index: Int) {
        entries.lastOrNull()?.onSelect?.invoke(index)
    }

    private fun publish() {
        mutableSections.value = entries.lastOrNull()?.sections
    }
}
