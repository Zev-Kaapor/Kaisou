package com.kaisou.app.features.settings.data

// Android context and preferences
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

// How often the app looks for a newer version
enum class UpdateInterval(val hours: Long, @StringRes val titleRes: Int) {
    SixHours(6, R.string.updates_interval_6),
    TwelveHours(12, R.string.updates_interval_12),
    OneDay(24, R.string.updates_interval_24),
    OneWeek(168, R.string.updates_interval_168)
}

// What the settings of the updates are
data class UpdateSettings(
    // True while the app does not look for newer versions by itself
    val paused: Boolean = false,
    val interval: UpdateInterval = UpdateInterval.OneDay
)

// What the user chose about the updates, and the last check and the version the user said later to, kept on the phone. It is set up when
// the app starts, so the screens and the check that runs in the background read the same thing
object UpdateStore {
    private const val FileName = "kaisou_settings"
    private const val PausedKey = "updates_paused"
    private const val IntervalKey = "update_interval"
    private const val LastCheckKey = "last_update_check"
    private const val DismissedKey = "dismissed_update"

    private lateinit var preferences: SharedPreferences
    private val mutableUpdates = MutableStateFlow(UpdateSettings())

    val updates: StateFlow<UpdateSettings> = mutableUpdates.asStateFlow()

    fun init(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableUpdates.value = read()
    }

    fun setPaused(value: Boolean) {
        preferences.edit { putBoolean(PausedKey, value) }
        mutableUpdates.value = read()
    }

    fun setInterval(value: UpdateInterval) {
        preferences.edit { putString(IntervalKey, value.name) }
        mutableUpdates.value = read()
    }

    // When the last check was made, and the version the user said later to, so it is not asked again
    var lastCheck: Long
        get() = preferences.getLong(LastCheckKey, 0L)
        set(value) = preferences.edit { putLong(LastCheckKey, value) }

    var dismissedVersion: String?
        get() = preferences.getString(DismissedKey, null)
        set(value) = preferences.edit { putString(DismissedKey, value) }

    private fun read(): UpdateSettings =
        UpdateSettings(
            paused = preferences.getBoolean(PausedKey, false),
            interval = UpdateInterval.entries.firstOrNull { it.name == preferences.getString(IntervalKey, null) } ?: UpdateInterval.OneDay
        )
}
