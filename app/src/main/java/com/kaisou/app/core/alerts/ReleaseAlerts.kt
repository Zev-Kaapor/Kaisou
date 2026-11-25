package com.kaisou.app.core.alerts

// Android framework
import android.content.Context
import android.content.SharedPreferences
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The titles whose releases the user follows with the bell, kept on the phone until the notices are made
object ReleaseAlerts {
    // Name of the file of the choice and the key of the titles in it
    private const val FileName = "kaisou_alerts"
    private const val TitlesKey = "titles"

    private lateinit var preferences: SharedPreferences
    private val mutableTitles = MutableStateFlow<Set<Int>>(emptySet())
    val titles: StateFlow<Set<Int>> = mutableTitles.asStateFlow()

    // Opens the file of the choice and reads the titles that were followed
    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableTitles.value = preferences.getStringSet(TitlesKey, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    // Turns the bell of a title on or off, true when it is on now
    fun toggle(id: Int): Boolean {
        val on = id !in mutableTitles.value
        val next = if (on) mutableTitles.value + id else mutableTitles.value - id
        mutableTitles.value = next
        preferences.edit().putStringSet(TitlesKey, next.map { it.toString() }.toSet()).apply()
        return on
    }
}
