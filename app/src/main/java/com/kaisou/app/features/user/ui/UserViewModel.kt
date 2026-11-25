package com.kaisou.app.features.user.ui

// Android framework and lifecycle
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.rating.MangaAgeRepository
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.navigation.UserNameArg
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.MarkdownTranslator
import com.kaisou.app.features.user.data.UserActivity
import com.kaisou.app.features.user.data.UserRepository
import com.kaisou.app.features.user.data.UserUi

// Loads the page of a person when it is opened, and follows them or stops following them
class UserViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val name: String = checkNotNull(savedStateHandle[UserNameArg])
    private val repository = UserRepository(application)

    // Whether the page is the user's own, known by the name before the page comes, so the placeholder knows about the button
    val isSelf: Boolean = name.equals(AniListSession.viewer?.name, ignoreCase = true)
    private val mutableState = MutableStateFlow<Load<UserUi>>(Load.Loading)
    val state: StateFlow<Load<UserUi>> = mutableState.asStateFlow()

    // The last things the person did, asked once the page came, null while they come, shown as placeholders
    private val mutableRecent = MutableStateFlow<List<UserActivity>?>(null)
    val recent: StateFlow<List<UserActivity>?> = mutableRecent.asStateFlow()

    // How many follow them and how many they follow, null while they are coming, which come with the last things they did
    private val mutableCounts = MutableStateFlow<Pair<Int, Int>?>(null)
    val counts: StateFlow<Pair<Int, Int>?> = mutableCounts.asStateFlow()

    // Whether what they say of themselves is shown translated, until the user turns it off, and that text as each group comes back
    private val mutableTranslate = MutableStateFlow(true)
    val translate: StateFlow<Boolean> = mutableTranslate.asStateFlow()
    private val mutableAbout = MutableStateFlow<String?>(null)
    val about: StateFlow<String?> = mutableAbout.asStateFlow()

    // The section of the page that is open, which stays when the phone is turned
    var tab by mutableStateOf(UserTab.Overview)

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()
    private var turningFollow = false

    init {
        load()
    }

    // Asks for the page of the person
    fun load() {
        mutableState.value = Load.Loading
        viewModelScope.launch {
            val user = repository.user(name)
            mutableState.value = if (user == null) Load.Failed else Load.Done(user)
            if (user == null) return@launch

            // The text is put in the language of the app apart, so the rest of the page does not wait for it
            launch { translateAbout(user.about) }

            // The manga of the favourites are said all at once, so their ages come in one question and the cards find them ready
            val manga = (user.favourites.anime + user.favourites.manga).filter { it.rating.manga }.map { it.rating.anilistId }
            if (manga.isNotEmpty()) MangaAgeRepository.prefetch(manga)
            val tail = repository.tail(user.id)
            mutableCounts.value = tail.followers to tail.following
            mutableRecent.value = tail.recent
        }
    }

    // Turns the translation of what they say of themselves on or off
    fun toggleTranslate() {
        val on = !mutableTranslate.value
        mutableTranslate.value = on
        val about = (mutableState.value as? Load.Done)?.value?.about
        if (on && about != null && mutableAbout.value == null) viewModelScope.launch { translateAbout(about) }
    }

    // Translates what they say of themselves, group by group
    private suspend fun translateAbout(text: String) {
        if (!mutableTranslate.value || mutableAbout.value != null || text.isBlank() || !AppLanguage.needsTranslation) return
        mutableAbout.value = MarkdownTranslator.translate(text, AppLanguage.translationCode) { partial -> mutableAbout.value = partial }
    }

    // The button changes at once and goes back if AniList does not take it. The island says what happened
    fun toggleFollow() {
        val before = (mutableState.value as? Load.Done)?.value ?: return
        if (turningFollow) return
        turningFollow = true
        setFollowing(!before.isFollowing)
        viewModelScope.launch {
            val application = getApplication<Application>()
            val now = repository.toggleFollow(name, before.id)
            if (now != null) {
                setFollowing(now)
                Toasts.show(application.getString(if (now) R.string.user_follow_added else R.string.user_follow_removed, before.name))
            } else {
                setFollowing(before.isFollowing)
                Toasts.show(application.getString(R.string.user_follow_failed), isError = true)
            }
            turningFollow = false
        }
    }

    // Sets whether the user follows the person
    private fun setFollowing(value: Boolean) {
        mutableState.update { state -> if (state is Load.Done) Load.Done(state.value.copy(isFollowing = value)) else state }
    }
}
