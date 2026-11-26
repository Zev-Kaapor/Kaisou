package com.kaisou.app.features.user.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.navigation.UserNameArg
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.user.data.UserListSection
import com.kaisou.app.features.user.data.UserListStore

// The lists of anime and of manga of a person, as the section of the lists shows them, each as it was asked
data class UserListsUi(val anime: Load<List<UserListSection>> = Load.Idle, val manga: Load<List<UserListSection>> = Load.Idle)

// Asks for the lists of a person the first time the section is shown, the anime and the manga in one question
class UserListsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    val name: String = checkNotNull(savedStateHandle[UserNameArg])
    private val mutableState = MutableStateFlow(UserListsUi())
    val state: StateFlow<UserListsUi> = mutableState.asStateFlow()

    // Asks for the lists that were not asked yet, or that failed the last time
    fun ensureLoaded() {
        val now = mutableState.value
        if (now.anime is Load.Idle || now.anime is Load.Failed) load(manga = false)
        if (now.manga is Load.Idle || now.manga is Load.Failed) load(manga = true)
    }

    // Asks for the lists of one kind
    private fun load(manga: Boolean) {
        if (name.isBlank()) return
        mutableState.update { if (manga) it.copy(manga = Load.Loading) else it.copy(anime = Load.Loading) }
        viewModelScope.launch {
            val found = UserListStore.lists(getApplication(), name, manga)
            val result = if (found == null) Load.Failed else Load.Done(found)
            mutableState.update { if (manga) it.copy(manga = result) else it.copy(anime = result) }
        }
    }
}
