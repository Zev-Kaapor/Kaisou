package com.kaisou.app.features.home.ui

// Animation
import androidx.compose.animation.animateColorAsState
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
// View model
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.HomeShelf
import com.kaisou.app.features.home.data.HomeStatus
import com.kaisou.app.features.home.data.HomeUiState
import com.kaisou.app.features.home.data.PosterRatings
import com.kaisou.app.features.home.ui.components.HomeHeader
import com.kaisou.app.features.home.ui.components.HomeHeroCarousel
import com.kaisou.app.features.home.ui.components.HomeSkeleton
import com.kaisou.app.features.home.ui.components.NewEpisodesShelf
import com.kaisou.app.features.home.ui.components.PosterShelf
import com.kaisou.app.features.home.ui.components.StoriesShelf

// Home of the user: the featured titles and the shelves, from what is happening now to what comes next
@Composable
fun HomeScreen(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    // The header is clear over the top of the art of the hero and turns solid before the text of the hero
    // gets under it, so its icons never sit over the text and the buttons
    val solidOffset = LocalWindowInfo.current.containerSize.width * HomeDimens.HeroAspect * HomeDimens.HeaderSolidFraction
    val heroGone by remember(solidOffset) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > solidOffset }
    }
    // The end of the list stays clear of the navigation bar, with the same room as between the shelves
    val listPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + HomeDimens.SectionSpacing)
    val headerColor by animateColorAsState(
        targetValue = if (heroGone) MaterialTheme.colorScheme.background else Color.Transparent,
        label = "headerColor"
    )

    Box(modifier = modifier) {
        when (state.status) {
            HomeStatus.Loading -> HomeSkeleton()
            HomeStatus.Error -> HomeError(onRetry = viewModel::load)
            HomeStatus.Content -> LazyColumn(
                state = listState,
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(HomeDimens.SectionSpacing)
            ) {
                if (state.heroes.isNotEmpty()) {
                    item(key = "hero") {
                        HomeHeroCarousel(
                            heroes = state.heroes,
                            arts = state.heroArts,
                            posterSize = state.posterSize,
                            onPlay = {},
                            onAddToList = {}
                        )
                    }
                }
                // Now: what the friends are doing
                if (state.stories.isNotEmpty()) {
                    item(key = "stories") { StoriesShelf(stories = state.stories, onOpenStory = {}, onPostStatus = {}) }
                }
                // Resume: where the user stopped, the episodes that just aired and the sequels they have not started
                shelf(state, HomeShelf.Watching, viewModel.ratings)
                shelf(state, HomeShelf.Reading, viewModel.ratings)
                if (state.newEpisodes.isNotEmpty()) {
                    item(key = "episodes") { NewEpisodesShelf(items = state.newEpisodes, onOpen = {}) }
                }
                shelf(state, HomeShelf.MissingSequels, viewModel.ratings)
                // For the user: what people recommend after their taste and what is rising
                shelf(state, HomeShelf.RecommendedAnime, viewModel.ratings)
                shelf(state, HomeShelf.Trending, viewModel.ratings)
                shelf(state, HomeShelf.RecommendedManga, viewModel.ratings)
                // The queue of the user
                shelf(state, HomeShelf.PlannedAnime, viewModel.ratings)
                shelf(state, HomeShelf.PlannedManga, viewModel.ratings)
                // What comes next: what was added to the catalog and, to close, the next season
                shelf(state, HomeShelf.NewAnime, viewModel.ratings)
                shelf(state, HomeShelf.NewManga, viewModel.ratings)
                shelf(state, HomeShelf.NextSeason, viewModel.ratings)
            }
        }
        HomeHeader(
            avatarUrl = avatarUrl,
            containerColor = headerColor,
            onNotifications = {},
            onSearch = {},
            onProfile = {},
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// Shown when AniList could not be reached and there is nothing to show, a tap asks again
@Composable
private fun HomeError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onRetry)
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.home_retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// A shelf of posters, when it has something to show
private fun LazyListScope.shelf(state: HomeUiState, shelf: HomeShelf, ratings: PosterRatings) {
    val items = state.shelves[shelf]
    if (items.isNullOrEmpty()) return
    item(key = shelf.name) {
        PosterShelf(title = stringResource(shelf.title), items = items, ratings = ratings, onOpen = {})
    }
}
