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

// A sign that the list of the user changed on AniList. What was kept of the cards of the titles is taken away, so the score and the place
// on the list they show are not behind, and the screens that show what is on the list, like the Home, ask again when they come back instead
// of waiting for the time between two questions
object ListChanges {
    private val mutableVersion = MutableStateFlow(0)
    val version: StateFlow<Int> = mutableVersion.asStateFlow()

    suspend fun changed() {
        withContext(Dispatchers.IO) {
            AppStore.clear(TitleRepository.ExtrasSpace)
            AppStore.clear(TitleRepository.SeriesSpace)
        }
        mutableVersion.update { it + 1 }
    }
}
