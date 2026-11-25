package com.kaisou.app.features.title.ui

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
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.navigation.CardsKindArg
import com.kaisou.app.core.navigation.CardsMangaArg
import com.kaisou.app.core.navigation.CardsOrder
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.TitleRepository

// A title of the list with the kind of tie it has to the one of the page, which the filter picks by, none in the order of the series
data class CardsItem(val card: CardUi, val group: String?)

// The titles that came, and the place in them of the one of the page, which the order of the series opens on
class CardsList(val items: List<CardsItem>, val current: Int)

// What the page of all the titles tied to a title shows: whether they came and the kind of tie that is chosen
data class CardsUi(val list: Load<CardsList> = Load.Loading, val group: String? = null)

// Asks for all the titles tied to a title, or its whole series in order, from what the title page kept, asking AniList for nothing
class TitleCardsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    val order: Boolean = savedStateHandle.get<String>(CardsKindArg) == CardsOrder
    val manga: Boolean = savedStateHandle.get<Boolean>(CardsMangaArg) ?: false
    private val repository = TitleRepository(application)
    private val mutableState = MutableStateFlow(CardsUi())
    val state: StateFlow<CardsUi> = mutableState.asStateFlow()

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        load()
    }

    // Asks for the list
    fun load() {
        mutableState.update { it.copy(list = Load.Loading) }
        viewModelScope.launch {
            val details = repository.extras(id)?.details
            val relations = details?.relations
            val list = when {
                relations == null -> null
                order -> repository.watchOrder(id, manga, relations, details.malId)?.let { found -> CardsList(found.cards.map { CardsItem(it, null) }, found.current) }
                else -> CardsList(relations.map { CardsItem(it.card, it.label) }, 0)
            }
            mutableState.update { it.copy(list = if (list == null) Load.Failed else Load.Done(list)) }
        }
    }

    // Shows only the titles with one kind of tie, or all of them with none
    fun chooseGroup(group: String?) {
        scroll.index = 0
        scroll.offset = 0
        mutableState.update { it.copy(group = group) }
    }
}
