package com.kaisou.app.features.title.ui

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.SimpleCard
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.size
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.features.settings.ui.components.Choice
import com.kaisou.app.features.settings.ui.components.ChoiceSheet
import androidx.compose.foundation.layout.Spacer
import com.kaisou.app.features.title.ui.components.PickerPill
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.first

// All the titles tied to a title, or its whole series in the order to take it, in a grid. The relations can be told apart by the kind of tie
// with the buttons over the grid, and the order of the series has the place of each title on its picture and opens where the title of the
// page is
@Composable
fun TitleCardsScreen(onBack: () -> Unit, onOpenTitle: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: TitleCardsViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState(viewModel.scroll.index, viewModel.scroll.offset)
    val loaded = (state.list as? Load.Done)?.value
    val groups = loaded?.items?.mapNotNull { it.group }?.distinct().orEmpty()

    // The position is kept by the view model, so it is the same when the phone is turned
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            viewModel.scroll.index = index
            viewModel.scroll.offset = offset
        }
    }
    // The order of the series opens where the title of the page is, the first time, with its card in the middle of the screen and not at its top
    val density = LocalDensity.current
    LaunchedEffect(loaded) {
        if (loaded != null && viewModel.order && viewModel.scroll.index == 0 && loaded.current > 0) {
            gridState.scrollToItem(loaded.current)
            val item = snapshotFlow { gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == loaded.current } }.first { it != null }!!
            val edge = with(density) { dimens.spaceMedium.roundToPx() }
            gridState.scrollToItem(loaded.current, -((gridState.layoutInfo.viewportSize.height - item.size.height) / 2 - edge))
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
                text = stringResource(
                    when {
                        !viewModel.order -> R.string.title_section_relations
                        viewModel.manga -> R.string.title_section_read_order
                        else -> R.string.title_section_watch_order
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
            // The kind of tie that is shown, in the line of the title at its end, only when there is more than one
            if (groups.size > 1) {
                val allLabel = stringResource(R.string.title_cards_all)
                val filterTitle = stringResource(R.string.title_cards_filter)
                Spacer(modifier = Modifier.weight(1f))
PickerPill(
                    text = state.group ?: allLabel,
                    onClick = {
                        OverlaySheet.show {
                            ChoiceSheet(
                                title = filterTitle,
                                choices = listOf(Choice(allLabel, state.group == null) { viewModel.chooseGroup(null) }) +
                                    groups.map { group -> Choice(group, state.group == group) { viewModel.chooseGroup(group) } },
                                onDismiss = OverlaySheet::dismiss
                            )
                        }
                    }
                )
            }
        }
        when (state.list) {
            is Load.Failed -> DetailsError(onRetry = viewModel::load)
            is Load.Done -> {
                val shown = loaded?.items?.filter { state.group == null || it.group == state.group }.orEmpty()
                // With no filter on, the relations come in groups, each with the name of its kind over it
                val grouped = !viewModel.order && state.group == null && groups.size > 1
                Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(HomeDimens.PosterWidth + dimens.spaceSmall),
                        modifier = Modifier.fillMaxSize().scrollIndicator(gridState),
                        contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
                    ) {
                        if (grouped) {
                            shown.groupBy { it.group }.forEach { (group, inGroup) ->
                                item(key = "group-$group", span = { GridItemSpan(maxLineSpan) }) {
                                    Text(text = group.orEmpty(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                }
                                items(inGroup, key = { "$group-${it.card.anilistId}" }) { item ->
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                        SimpleCard(card = item.card, onClick = { onOpenTitle(item.card.anilistId) })
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(shown, key = { _, item -> item.card.anilistId }) { index, item ->
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                    SimpleCard(
                                        card = item.card,
                                        onClick = { onOpenTitle(item.card.anilistId) },
                                        badge = if (viewModel.order) (index + 1).toString() else null,
                                        shine = viewModel.order && index == loaded?.current
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> CardsGridSkeleton(grouped = !viewModel.order)
        }
    }
}
