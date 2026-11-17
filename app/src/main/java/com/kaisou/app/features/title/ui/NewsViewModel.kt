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
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.core.navigation.NewsMalArg
import com.kaisou.app.core.navigation.NewsMangaArg
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.NewsClient
import com.kaisou.app.features.title.data.TitleNews

// Loads all the news of a title for the page that lists them. They were just asked for by the page of the title, so they come from what
// is kept on the phone
class NewsViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val anilistId: Int = checkNotNull(savedStateHandle[TitleIdArg])
    private val malId: Int? = savedStateHandle.get<Int>(NewsMalArg)?.takeIf { it > 0 }
    private val manga: Boolean = savedStateHandle.get<Boolean>(NewsMangaArg) ?: false
    private val mutableState = MutableStateFlow<Load<List<TitleNews>>>(Load.Loading)
    val state: StateFlow<Load<List<TitleNews>>> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.value = Load.Loading
        viewModelScope.launch {
            val news = NewsClient.news(anilistId, malId, manga)
            mutableState.value = if (news == null) Load.Failed else Load.Done(news)
        }
    }
}
