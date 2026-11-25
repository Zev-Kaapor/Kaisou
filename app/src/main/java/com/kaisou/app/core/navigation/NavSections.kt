package com.kaisou.app.core.navigation

// Coroutines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// UI styling and utilities
import androidx.compose.ui.graphics.vector.ImageVector

// The sections a screen asks the bar to show in place of the main destinations: their names, their icons and the one that is open
data class NavSections(val labels: List<String>, val icons: List<ImageVector>, val selected: Int)

// Where the screens put their sections for the bar: the last one to come is shown, and a screen removes only what it put
object NavSectionsHost {
    // A screen that put its sections, and what to do when one is chosen
    private class Entry(val owner: Any, var sections: NavSections, var onSelect: (Int) -> Unit)

    private val entries = mutableListOf<Entry>()
    private val mutableSections = MutableStateFlow<NavSections?>(null)
    val sections: StateFlow<NavSections?> = mutableSections.asStateFlow()

    // Puts or updates the sections of a screen, which keeps its place so one going out does not take the bar from the one coming
    fun show(owner: Any, sections: NavSections, onSelect: (Int) -> Unit) {
        val entry = entries.firstOrNull { it.owner === owner } ?: Entry(owner, sections, onSelect).also { entries += it }
        entry.sections = sections
        entry.onSelect = onSelect
        publish()
    }

    // Takes away the sections a screen put
    fun clear(owner: Any) {
        entries.removeAll { it.owner === owner }
        publish()
    }

    // A choice of the bar goes to the screen that is shown
    fun select(index: Int) {
        entries.lastOrNull()?.onSelect?.invoke(index)
    }

    // Tells the bar the sections of the last screen
    private fun publish() {
        mutableSections.value = entries.lastOrNull()?.sections
    }
}
