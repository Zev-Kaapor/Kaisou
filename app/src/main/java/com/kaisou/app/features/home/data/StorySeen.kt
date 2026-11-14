package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
import androidx.core.content.edit

// The activities of the stories the user already saw, kept on the device. Only the newest ones are kept, the old ones are out of
// the stories anyway
class StorySeen(context: Context) {
    private val preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)

    fun ids(): Set<Int> = preferences.getStringSet(Key, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    // Marks one as seen and gives all that are kept
    fun add(id: Int): Set<Int> {
        val kept = (ids() + id).sorted().takeLast(MaxKept)
        preferences.edit { putStringSet(Key, kept.map { it.toString() }.toSet()) }
        return kept.toSet()
    }

    private companion object {
        const val FileName = "kaisou_stories"
        const val Key = "seen"
        const val MaxKept = 200
    }
}
