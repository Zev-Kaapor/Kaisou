package com.kaisou.app

// Android framework
import android.app.Application
// Project resources
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.features.settings.data.UpdateStore
// Java files
import java.io.File

// Sets up what has to be there before any screen
class KaisouApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppStore.init(File(cacheDir, "store"))
        UpdateStore.init(this)
    }
}
