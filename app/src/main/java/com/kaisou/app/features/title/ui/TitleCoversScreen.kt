package com.kaisou.app.features.title.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.MangaArtClient
import com.kaisou.app.features.title.data.MangaCover
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.CoverTile
import com.kaisou.app.features.title.ui.components.DetailsError
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kaisou.app.features.title.ui.components.CoversViewer

// Gets the covers of a manga that the title page kept a moment ago, so opening this page asks MangaDex for nothing
class TitleCoversViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    private val mutableState = MutableStateFlow<Load<List<MangaCover>>>(Load.Loading)
    val state: StateFlow<Load<List<MangaCover>>> = mutableState.asStateFlow()

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    init {
        load()
    }

    // Asks for the covers
    fun load() {
        mutableState.value = Load.Loading
        viewModelScope.launch {
            val covers = MangaArtClient.kept(id, AppLanguage.code, AppLanguage.region)
            mutableState.value = if (covers.isNullOrEmpty()) Load.Failed else Load.Done(covers)
        }
    }
}

// All the covers of the volumes of a manga in a grid, each with its volume and language, a tap opening the picture
@Composable
fun TitleCoversScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: TitleCoversViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState(viewModel.scroll.index, viewModel.scroll.offset)
    var viewing by rememberSaveable { mutableStateOf<Int?>(null) }

    // The position is kept by the view model, so it is the same when the phone is turned
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            viewModel.scroll.index = index
            viewModel.scroll.offset = offset
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAction(icon = KaisouIcons.Back, contentDescription = stringResource(R.string.cd_back), onClick = onBack)
            Text(
                text = stringResource(R.string.title_section_covers),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        when (val loaded = state) {
            is Load.Done -> Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                viewing?.let { CoversViewer(loaded.value, startIndex = it, onDismiss = { viewing = null }) }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(CoversPerRow),
                    modifier = Modifier.fillMaxSize().scrollIndicator(gridState),
                    contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
                ) {
                    itemsIndexed(loaded.value, key = { _, cover -> cover.original }) { index, cover ->
                        CoverTile(cover, onClick = { viewing = index }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            is Load.Failed -> DetailsError(onRetry = viewModel::load)
            else -> CardsGridSkeleton(grouped = false)
        }
    }
}

// How many covers are in each line of the page, whatever the width of the screen
private const val CoversPerRow = 3
