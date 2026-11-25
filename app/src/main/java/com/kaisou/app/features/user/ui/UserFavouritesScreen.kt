package com.kaisou.app.features.user.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
// Material 3 components
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.ui.CastCard
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.CastRowsSkeleton
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.SimpleCard
import com.kaisou.app.features.user.data.FavouriteKind

// All the favourites of one kind a page at a time: posters in a grid, characters and staff in cast cards, studios in lines
@Composable
fun UserFavouritesScreen(
    onBack: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserFavouritesViewModel = viewModel()
) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState(viewModel.scroll.index, viewModel.scroll.offset)
    val titles = viewModel.kind == FavouriteKind.Anime || viewModel.kind == FavouriteKind.Manga
    val count = state.cards.size + state.people.size + state.studios.size

    // The position is kept by the view model, so it is the same when the phone is turned
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            viewModel.scroll.index = index
            viewModel.scroll.offset = offset
        }
    }

    // The next page is asked for when the last rows are on the screen
    LaunchedEffect(gridState, count) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= count - LoadAhead) viewModel.loadMore()
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
                text = stringResource(viewModel.kind.title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        when {
            state.failed && state.isEmpty -> DetailsError(onRetry = viewModel::loadMore)
            state.isEmpty && state.hasMore -> if (titles) CardsGridSkeleton(grouped = false) else CastRowsSkeleton(SkeletonRows)
            else -> LazyVerticalGrid(
                state = gridState,
                columns = if (titles) GridCells.Adaptive(HomeDimens.PosterWidth + dimens.spaceSmall) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize().scrollIndicator(gridState),
                contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(if (titles) dimens.spaceMedium else dimens.spaceSmall)
            ) {
                items(state.cards, key = { "title-${it.anilistId}" }) { card ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        SimpleCard(card = card, onClick = { onOpenTitle(card.anilistId) })
                    }
                }
                items(state.people, key = { it.url }) { person -> CastCard(person, onOpenProfile) }
                items(state.studios, key = { "studio-${it.ref.id}" }) { studio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.elevated)
                            .clickable(role = Role.Button) { onOpenProfile(studio.ref) }
                            .padding(dimens.spaceMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = studio.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                when {
                    state.failed -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) { DetailsError(onRetry = viewModel::loadMore) }
                    state.loading || state.hasMore -> item(key = "loading-more", span = { GridItemSpan(maxLineSpan) }) { CastRowsSkeleton(SkeletonMore) }
                }
            }
        }
    }
}

// How many rows before the end the next page is asked, and how many placeholders stand in for the first page and the next
private const val LoadAhead = 4
private const val SkeletonRows = 8
private const val SkeletonMore = 2
