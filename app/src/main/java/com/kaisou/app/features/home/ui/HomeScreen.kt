package com.kaisou.app.features.home.ui

// Compose animation
import androidx.compose.animation.animateColorAsState
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
// View model
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniViewer
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.HomeStatus
import com.kaisou.app.features.home.ui.components.HomeHeader
import com.kaisou.app.features.home.ui.components.HomeHeroCarousel
import com.kaisou.app.features.home.ui.components.HomeHeroSkeleton

// Home of the user: the hero at the top with the header over it, the rest of the screen arrives piece by piece
@Composable
fun HomeScreen(
    viewer: AniViewer,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        // Standing up the art of the hero is a poster as tall as the screen is wide times a ratio, lying down it takes a share of
        // the height, so the next part of the Home shows under it
        val heroHeight = if (landscape) maxHeight * HomeDimens.HeroLandscapeHeightFraction else maxWidth * HomeDimens.HeroAspect

        // The header is clear over the art of the hero and turns solid before the details of the hero get under it, so its
        // icons never sit over the text and the buttons
        val solidOffset = with(LocalDensity.current) { heroHeight.toPx() } * HomeDimens.HeaderSolidFraction
        val heroGone by remember(solidOffset) {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > solidOffset }
        }
        val headerColor by animateColorAsState(
            targetValue = if (heroGone) MaterialTheme.colorScheme.background else Color.Transparent,
            label = "headerColor"
        )

        when (state.status) {
            HomeStatus.Loading -> HomeHeroSkeleton(height = heroHeight, landscape = landscape)
            HomeStatus.Error -> HomeError(onRetry = viewModel::load)
            HomeStatus.Content -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(HomeDimens.SectionSpacing)
            ) {
                item(key = "hero") {
                    HomeHeroCarousel(
                        heroes = state.heroes,
                        index = state.heroIndex,
                        onIndexChange = viewModel::showHero,
                        art = state.art,
                        posterSize = state.posterSize,
                        backdropSize = state.backdropSize,
                        logoSize = state.logoSize,
                        height = heroHeight,
                        landscape = landscape,
                        onPlay = {},
                        onAddToList = {}
                    )
                }
            }
        }

        HomeHeader(
            viewer = viewer,
            containerColor = headerColor,
            onNotifications = {},
            onSearch = {},
            onProfile = onProfile,
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
