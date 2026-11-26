package com.kaisou.app

// Android framework
import android.app.Application
// Project resources
import com.kaisou.app.core.alerts.ReleaseAlerts
import com.kaisou.app.core.language.TagOrderPreference
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.net.Gates
import com.kaisou.app.features.settings.data.UpdateStore
import com.kaisou.app.features.user.data.ListPreference
// Java files and math
import java.io.File
import kotlin.math.ceil

// Sets up what has to be there before any screen
class KaisouApplication : Application() {
    // Starts what the whole app needs before the first screen opens
    override fun onCreate() {
        super.onCreate()
        AppStore.init(File(cacheDir, "store"))
        UpdateStore.init(this)
        ReleaseAlerts.init(this)
        TagOrderPreference.init(this)
        ListPreference.init(this)

        // When AniList asks the app to rest, the island says so and for how long, since pages would otherwise just take long or fail
        Gates.AniList.onRest = { millis ->
            val seconds = ceil(millis / MillisPerSecond).toInt().coerceAtLeast(1)
            Toasts.show(
                message = getString(R.string.rate_limit_title),
                subtitle = resources.getQuantityString(R.plurals.rate_limit_wait, seconds, seconds),
                isError = true
            )
        }
    }

    // Constants of the application
    private companion object {
        // Milliseconds in a second, to say the rest of the limit in seconds
        const val MillisPerSecond = 1000.0
    }
}
