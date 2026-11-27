package com.kaisou.app.features.settings.data

// Android context and preferences
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// What the user wants of the titles AniList marks for adults: all of them, only them, or none of them
enum class AdultMode { Show, Only, Hide }

// The choice about adult content, kept on the phone and told to every part of the app that shows titles
object AdultContent {
    // Name of the file of the settings and the key of the choice
    private const val FileName = "kaisou_settings"
    private const val ModeKey = "adult_mode"

    private lateinit var preferences: SharedPreferences
    private val mutableMode = MutableStateFlow(AdultMode.Show)
    val mode: StateFlow<AdultMode> = mutableMode.asStateFlow()

    // Opens the settings and reads the choice
    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableMode.value = AdultMode.entries.firstOrNull { it.name == preferences.getString(ModeKey, null) } ?: AdultMode.Show
    }

    // Shows only the adult titles when on, and everything again when off, which turns the hiding off
    fun setOnly(on: Boolean) = set(if (on) AdultMode.Only else AdultMode.Show)

    // Hides the adult titles when on, and shows everything again when off, which turns the showing of only them off
    fun setHide(on: Boolean) = set(if (on) AdultMode.Hide else AdultMode.Show)

    // Tells whether a title stays on the screen, by whether AniList marks it for adults
    fun allows(adult: Boolean): Boolean = when (mutableMode.value) {
        AdultMode.Show -> true
        AdultMode.Only -> adult
        AdultMode.Hide -> !adult
    }

    // Changes the choice and keeps it
    private fun set(value: AdultMode) {
        preferences.edit { putString(ModeKey, value.name) }
        mutableMode.value = value
    }
}
