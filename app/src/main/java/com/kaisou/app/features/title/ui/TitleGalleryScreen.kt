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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
// Material 3 components
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.artwork.TitleGallery
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.navigation.GalleryKindArg
import com.kaisou.app.core.navigation.TitleIdArg
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.title.data.GalleryKind
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.GalleryTile
import com.kaisou.app.features.title.ui.components.GalleryViewer

// Gets the gallery of an anime that the title page kept a moment ago, so opening this page asks the sites for nothing
class TitleGalleryViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[TitleIdArg])
    private val mutableState = MutableStateFlow<Load<TitleGallery>>(Load.Loading)
    val state: StateFlow<Load<TitleGallery>> = mutableState.asStateFlow()

    // The kind that opens first, from the address
    val firstKind: GalleryKind = GalleryKind.byPath(savedStateHandle[GalleryKindArg])

    init {
        load()
    }

    // Asks for the gallery
    fun load() {
        mutableState.value = Load.Loading
        viewModelScope.launch {
            val gallery = ArtworkRepository.Shared.keptGallery(id, AppLanguage.code)
            mutableState.value = if (gallery == null || gallery.isEmpty) Load.Failed else Load.Done(gallery)
        }
    }
}

// The posters, backdrops and logos of an anime in a grid, a chip for each kind, a tap opening the picture over the whole screen
@Composable
fun TitleGalleryScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: TitleGalleryViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    var kind by rememberSaveable { mutableStateOf(viewModel.firstKind) }
    var viewing by rememberSaveable { mutableStateOf<Int?>(null) }

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
                text = stringResource(R.string.title_section_gallery),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        when (val loaded = state) {
            is Load.Done -> {
                val kinds = GalleryKind.entries.filter { it.of(loaded.value).isNotEmpty() }
                val shown = if (kind in kinds) kind else kinds.first()
                val images = shown.of(loaded.value)

                // A chip for each kind that has pictures, the grid under it showing the one that is picked
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    kinds.forEach { option ->
                        FilterChip(selected = option == shown, onClick = { kind = option; viewing = null }, label = { Text(stringResource(option.label)) })
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    viewing?.let { GalleryViewer(images, startIndex = it, onDismiss = { viewing = null }) }
                    val gridState = rememberLazyGridState()
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(if (shown == GalleryKind.Posters) TitleDimens.GalleryPosterColumn else TitleDimens.GalleryWideColumn),
                        modifier = Modifier.fillMaxSize().scrollIndicator(gridState),
                        contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
                    ) {
                        itemsIndexed(images, key = { _, image -> image.url }) { index, image ->
                            GalleryTile(image, shown, onClick = { viewing = index }, modifier = Modifier.fillMaxWidth())
                        }

                        // The sites that gave the pictures are named once, under all of them
                        item(key = "credit", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.title_gallery_credit),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is Load.Failed -> DetailsError(onRetry = viewModel::load)
            else -> CardsGridSkeleton(grouped = false)
        }
    }
}
