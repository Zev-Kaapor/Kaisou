package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The parts of the Home the user turned on and the ones turned off, which tell for each part whether it shows
data class HomeLayoutState(val on: Set<String> = emptySet(), val off: Set<String> = emptySet()) {
    // Tells whether a part shows: what the user chose, or else what the part starts as
    fun shown(part: String): Boolean = part in on || (part !in off && part !in HomeLayout.StartsOff)
}

// What the user wants to see on the Home: the stories and each shelf, kept on the phone, the lists of states starting off
object HomeLayout {
    // The name of the part that holds the stories, which sits with the names of the shelves
    const val Stories = "Stories"

    // The shelves of the states of the list start off, so the Home keeps its look and does not ask for them until they are wanted
    val StartsOff: Set<String> = setOf(
        ShelfId.CompletedTv, ShelfId.CompletedMovie, ShelfId.CompletedManga, ShelfId.PausedAnime, ShelfId.PausedManga, ShelfId.DroppedAnime, ShelfId.DroppedManga
    ).map { it.name }.toSet()

    // Name of the file and the keys of the parts turned on and off
    private const val FileName = "kaisou_home"
    private const val OnKey = "layout_on"
    private const val OffKey = "layout_off"

    private lateinit var preferences: SharedPreferences
    private val mutableState = MutableStateFlow(HomeLayoutState())
    val state: StateFlow<HomeLayoutState> = mutableState.asStateFlow()

    // Opens the file and reads the choices
    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableState.value = HomeLayoutState(
            on = preferences.getStringSet(OnKey, emptySet()).orEmpty().toSet(),
            off = preferences.getStringSet(OffKey, emptySet()).orEmpty().toSet()
        )
    }

    // Turns a part of the Home on or off and keeps the choice
    fun setShown(part: String, shown: Boolean) {
        val now = mutableState.value
        val next = if (shown) HomeLayoutState(now.on + part, now.off - part) else HomeLayoutState(now.on - part, now.off + part)
        if (next == now) return
        mutableState.value = next
        preferences.edit {
            putStringSet(OnKey, next.on)
            putStringSet(OffKey, next.off)
        }
    }
}
