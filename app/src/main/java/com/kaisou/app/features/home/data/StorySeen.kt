package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
import androidx.core.content.edit

// The activities of the stories the user already saw, kept on the device, only the newest
class StorySeen(context: Context) {
    private val preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)

    // Gives the ids of the stories seen
    fun ids(): Set<Int> = preferences.getStringSet(Key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    // Marks one as seen and gives all that are kept
    fun add(id: Int): Set<Int> {
        val kept = (ids() + id).sorted().takeLast(MaxKept)
        preferences.edit { putStringSet(Key, kept.map { it.toString() }.toSet()) }
        return kept.toSet()
    }

    // Constants of the store of seen stories
    private companion object {
        // Name of the file, the key, and how many seen stories are kept
        const val FileName = "kaisou_stories"
        const val Key = "seen"
        const val MaxKept = 200
    }
}
