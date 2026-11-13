package com.kaisou.app.features.home.ui.components

// Animation
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.R
import com.kaisou.app.core.artwork.HeroArt
import com.kaisou.app.core.artwork.posterRequest
import com.kaisou.app.core.designsystem.components.ArtPlaceholder
import com.kaisou.app.core.designsystem.components.OutlineIconButton
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.components.RatingBadge
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.SkeletonLines
import com.kaisou.app.core.designsystem.components.TitleLogo
import com.kaisou.app.core.designsystem.components.WeeklyReleaseBadge
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.HeroUi
// Image loading
import coil3.compose.AsyncImage
import coil3.size.Size
// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Featured titles one at a time, one fades into the next after its time or on a swipe
@Composable
fun HomeHeroCarousel(
    heroes: List<HeroUi>,
    arts: Map<Int, HeroArt>,
    posterSize: Size,
    onPlay: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // The art is a poster, as tall as the screen is wide times the ratio
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val windowWidth = with(density) { containerWidth.toDp() }
    val heroHeight = windowWidth * HomeDimens.HeroAspect
    // The artwork comes from the Home, a title that is not in it yet is still loading
    val currentArts by rememberUpdatedState(arts)
    val swipeThreshold = with(density) { HomeDimens.HeroSwipeThreshold.toPx() }
    var index by rememberSaveable(heroes) { mutableIntStateOf(0) }

    // Each title gets its time once its artwork is ready, then the next one fades in, a swipe restarts the time
    LaunchedEffect(heroes, index) {
        snapshotFlow { currentArts.containsKey(heroes[index].anilistId) }.first { it }
        delay(HomeDimens.HeroAdvanceMillis)
        index = (index + 1) % heroes.size
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .pointerInput(heroes.size) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onHorizontalDrag = { _, delta -> dragged += delta },
                    onDragEnd = {
                        if (dragged < -swipeThreshold) index = (index + 1) % heroes.size
                        if (dragged > swipeThreshold) index = (index - 1 + heroes.size) % heroes.size
                    }
                )
            }
    ) {
        Crossfade(
            targetState = index,
            animationSpec = tween(HomeDimens.HeroFadeMillis),
            label = "hero"
        ) { page ->
            HomeHero(
                hero = heroes[page],
                art = arts[heroes[page].anilistId],
                posterSize = posterSize,
                onPlay = onPlay,
                onAddToList = onAddToList,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// One featured title: the art fading into the page, with the logo and the details anchored to its bottom
@Composable
private fun HomeHero(
    hero: HeroUi,
    art: HeroArt?,
    posterSize: Size,
    onPlay: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val background = MaterialTheme.colorScheme.background
    Box(modifier = modifier) {
        ArtPlaceholder(colors = hero.artColors, modifier = Modifier.fillMaxSize())
        // The gradient stays under the poster, which is already in memory when the page comes up
        art?.posterUrl?.let { posterUrl ->
            AsyncImage(
                model = posterRequest(LocalContext.current, posterUrl, posterSize),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Dark veil so the header reads over any art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HomeDimens.HeroScrimHeight)
                .background(Brush.verticalGradient(listOf(KaisouTheme.colors.scrimStrong, Color.Transparent)))
        )
        // Fade into the page under the details
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        HomeDimens.HeroFadeStart to Color.Transparent,
                        HomeDimens.HeroFadeEnd to background
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding)
                .padding(bottom = HomeDimens.HeroInfoBottom)
        ) {
            TitleLogo(
                title = art?.title ?: hero.title,
                logo = art?.logo,
                loading = art == null,
                boxHeight = HomeDimens.HeroLogoHeight,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.padding(top = dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                art?.rating?.let { RatingBadge(it) }
                hero.releaseDay?.let { WeeklyReleaseBadge(it) }
                Text(
                    text = if (hero.year != null) {
                        stringResource(R.string.home_hero_meta, hero.year, hero.format, hero.genres)
                    } else {
                        stringResource(R.string.home_hero_meta_no_year, hero.format, hero.genres)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (art == null) {
                // The synopsis may still be translated, so the lines of AniList are not shown for a moment
                SkeletonHost(modifier = Modifier.padding(top = dimens.spaceSmall)) {
                    SkeletonLines(lines = HomeDimens.HeroSynopsisLines)
                }
            } else {
                Text(
                    text = art.synopsis ?: hero.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    // The buttons stay anchored at the bottom, so a short text just leaves the logo lower
                    maxLines = HomeDimens.HeroSynopsisLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = dimens.spaceSmall)
                )
            }
            Row(
                modifier = Modifier.padding(top = HomeDimens.HeroButtonsTop),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(
                    text = stringResource(if (hero.continuing) R.string.home_cta_continue else R.string.home_cta_watch, hero.nextEpisode),
                    icon = KaisouIcons.PlayOutline,
                    onClick = onPlay,
                    modifier = Modifier.weight(1f)
                )
                OutlineIconButton(
                    icon = KaisouIcons.Bookmark,
                    contentDescription = stringResource(R.string.home_add_to_list),
                    onClick = onAddToList
                )
            }
        }
    }
}
