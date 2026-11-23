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
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.navigation.UserFavouritesKindArg
import com.kaisou.app.core.navigation.UserNameArg
import com.kaisou.app.core.rating.MangaAgeRepository
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.title.data.TitlePerson
import com.kaisou.app.features.user.data.FavouriteKind
import com.kaisou.app.features.user.data.UserRepository
import com.kaisou.app.features.user.data.UserStudio

// What the page of all the favourites of one kind shows: what came, whichever the kind is, whether there are more to ask for, and whether a page is
// being asked for
data class UserFavouritesUi(
    val cards: List<CardUi> = emptyList(),
    val people: List<TitlePerson> = emptyList(),
    val studios: List<UserStudio> = emptyList(),
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val failed: Boolean = false
) {
    val isEmpty: Boolean get() = cards.isEmpty() && people.isEmpty() && studios.isEmpty()
}

// Asks for the favourites of one kind of a person a page at a time, the next one when the list gets to its end
class UserFavouritesViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val name: String = checkNotNull(savedStateHandle[UserNameArg])
    val kind: FavouriteKind = FavouriteKind.of(savedStateHandle[UserFavouritesKindArg])
    private val repository = UserRepository(application)
    private val mutableState = MutableStateFlow(UserFavouritesUi())
    val state: StateFlow<UserFavouritesUi> = mutableState.asStateFlow()
    private var page = 0

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        loadMore()
    }

    fun loadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        mutableState.value = now.copy(loading = true, failed = false)
        viewModelScope.launch {
            val next = repository.favourites(name, kind, page + 1)
            val before = mutableState.value
            mutableState.value = if (next == null) {
                before.copy(loading = false, failed = true)
            } else {
                page++
                // The manga of the page are said all at once, so their ages come in one question and the cards find them ready
                next.cards.filter { it.rating.manga }.map { it.rating.anilistId }.takeIf { it.isNotEmpty() }?.let(MangaAgeRepository::prefetch)
                before.copy(
                    cards = (before.cards + next.cards).distinctBy { it.anilistId },
                    people = (before.people + next.people).distinctBy { it.url },
                    studios = (before.studios + next.studios).distinctBy { it.ref.id },
                    hasMore = next.hasMore,
                    loading = false
                )
            }
        }
    }
}
