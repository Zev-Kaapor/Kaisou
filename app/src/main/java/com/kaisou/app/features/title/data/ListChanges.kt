package com.kaisou.app.features.title.data

// Kotlin coroutines and flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
// Project resources
import com.kaisou.app.core.cache.AppStore

// A sign that the list of the user changed: the cards kept are taken away and the screens that show the list ask again
object ListChanges {
    private val mutableVersion = MutableStateFlow(0)
    val version: StateFlow<Int> = mutableVersion.asStateFlow()

    // Announces the change of the list, forgetting what was kept of the cards
    suspend fun changed() {
        withContext(Dispatchers.IO) {
            AppStore.clear(TitleRepository.ExtrasSpace)
            AppStore.clear(TitleRepository.SeriesSpace)
        }
        mutableVersion.update { it + 1 }
    }
}
