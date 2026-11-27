package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The titles the user chose not to see on the Home, kept on the phone only, as Dantotsu does, and never sent to AniList
object HiddenOnHome {
    // Name of the file and the key of the set of ids
    private const val FileName = "kaisou_home"
    private const val IdsKey = "hidden"

    private lateinit var preferences: SharedPreferences
    private val mutableIds = MutableStateFlow<Set<Int>>(emptySet())
    val ids: StateFlow<Set<Int>> = mutableIds.asStateFlow()

    // Opens the file and reads the ids that were hidden
    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableIds.value = preferences.getStringSet(IdsKey, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    // Tells whether a title is hidden
    fun contains(id: Int): Boolean = id in mutableIds.value

    // Hides a title from the Home or shows it again, and keeps the choice
    fun set(id: Int, hidden: Boolean) {
        val next = if (hidden) mutableIds.value + id else mutableIds.value - id
        if (next == mutableIds.value) return
        mutableIds.value = next
        preferences.edit { putStringSet(IdsKey, next.map { it.toString() }.toSet()) }
    }
}
