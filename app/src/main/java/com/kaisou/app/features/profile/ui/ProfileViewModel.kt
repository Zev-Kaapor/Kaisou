package com.kaisou.app.features.profile.ui

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
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.navigation.ProfileKindArg
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.profile.data.ProfileKind
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.profile.data.ProfileRepository
import com.kaisou.app.features.profile.data.ProfileUi
import com.kaisou.app.features.title.data.Load

// Loads the page of a character, a person of the staff or a studio when it is opened, and turns its favourite on and off
class ProfileViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val ref = ProfileRef(ProfileKind.of(savedStateHandle[ProfileKindArg]), checkNotNull(savedStateHandle[TitleIdArg]))
    private val repository = ProfileRepository(application)
    private val mutableState = MutableStateFlow<Load<ProfileUi>>(Load.Loading)
    val state: StateFlow<Load<ProfileUi>> = mutableState.asStateFlow()

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()
    private var turningFavourite = false

    init {
        load()
    }

    fun load() {
        mutableState.value = Load.Loading
        viewModelScope.launch {
            val profile = repository.profile(ref)
            mutableState.value = if (profile == null) Load.Failed else Load.Done(profile)
        }
    }

    // The heart changes at once and goes back if AniList does not take it. The island says what happened
    fun toggleFavourite() {
        val before = (mutableState.value as? Load.Done)?.value ?: return
        if (turningFavourite) return
        turningFavourite = true
        setFavourite(!before.isFavourite)
        viewModelScope.launch {
            val application = getApplication<Application>()
            if (repository.toggleFavourite(ref)) {
                Toasts.show(application.getString(if (before.isFavourite) R.string.title_favourite_removed else R.string.title_favourite_added))
            } else {
                setFavourite(before.isFavourite)
                Toasts.show(application.getString(R.string.title_favourite_failed), isError = true)
            }
            turningFavourite = false
        }
    }

    private fun setFavourite(value: Boolean) {
        mutableState.update { state -> if (state is Load.Done) Load.Done(state.value.copy(isFavourite = value)) else state }
    }
}
