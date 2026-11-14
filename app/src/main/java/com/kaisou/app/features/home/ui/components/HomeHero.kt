package com.kaisou.app.features.home.ui.components

// Compose animation
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
// Image loading
import coil3.compose.AsyncImage
import coil3.size.Size
// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.artRequest
import com.kaisou.app.core.artwork.logoRequest
import com.kaisou.app.core.designsystem.components.AgeRatingBadge
import com.kaisou.app.core.designsystem.components.ButtonIcon
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.OutlineIconButton
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.components.TitleLogo
import com.kaisou.app.core.designsystem.components.WeeklyReleaseBadge
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.HeroArt
import com.kaisou.app.features.home.data.HeroUi
import com.kaisou.app.features.home.ui.HomeDimens

// What the carousel shows while the poster of the title that is on is not ready
private const val NotReady = -1

// Featured titles one at a time: one fades into the next after its time or on a swipe
@Composable
fun HomeHeroCarousel(
    heroes: List<HeroUi>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    art: Map<Int, HeroArt>,
    posterSize: Size,
    backdropSize: Size,
    logoSize: Size,
    height: Dp,
    landscape: Boolean,
    onPlay: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeThreshold = with(LocalDensity.current) { HomeDimens.HeroSwipeThreshold.toPx() }
    val currentIndexChange by rememberUpdatedState(onIndexChange)
    // The pictures are looked for while the first title waits, a title is only shown when they are ready
    val currentArt by rememberUpdatedState(art)

    // Each title gets its time once its poster is ready, then the next one fades in, a swipe restarts the time
    LaunchedEffect(heroes, index) {
        snapshotFlow { currentArt.containsKey(heroes[index].anilistId) }.first { it }
        delay(HomeDimens.HeroAdvanceMillis)
        currentIndexChange((index + 1) % heroes.size)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(heroes.size, index) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onHorizontalDrag = { _, delta -> dragged += delta },
                    onDragEnd = {
                        if (dragged < -swipeThreshold) currentIndexChange((index + 1) % heroes.size)
                        if (dragged > swipeThreshold) currentIndexChange((index - 1 + heroes.size) % heroes.size)
                    }
                )
            }
    ) {
        // Until the pictures of the title are ready the whole hero is the placeholder, and it fades into the hero when it is
        val shown = if (art.containsKey(heroes[index].anilistId)) index else NotReady
        Crossfade(
            targetState = shown,
            animationSpec = tween(HomeDimens.HeroFadeMillis),
            label = "hero"
        ) { page ->
            if (page == NotReady) {
                HomeHeroSkeleton(height = height, landscape = landscape)
            } else {
                HomeHero(
                    hero = heroes[page],
                    art = art[heroes[page].anilistId],
                    posterSize = posterSize,
                    backdropSize = backdropSize,
                    logoSize = logoSize,
                    landscape = landscape,
                    onPlay = onPlay,
                    onAddToList = onAddToList,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// One featured title: the art fading into the page, with the name and the details anchored to its bottom, on the left
// side when lying down
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHero(
    hero: HeroUi,
    art: HeroArt?,
    posterSize: Size,
    backdropSize: Size,
    logoSize: Size,
    landscape: Boolean,
    onPlay: () -> Unit,
    onAddToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Box(modifier = modifier) {
        // The poster is asked the way the preload did, so it comes from memory. Until it is ready the cover shows
        val context = LocalContext.current
        // Lying down the picture is the wide one, with the poster as the second choice. Standing up it is the poster
        val hasBackdrop = landscape && art?.backdropUrl != null
        val model = remember(art, hero.coverUrl, landscape) {
            val backdrop = if (landscape) art?.backdropUrl?.let { artRequest(context, it, backdropSize) } else null
            backdrop ?: art?.posterUrl?.let { artRequest(context, it, posterSize) } ?: hero.coverUrl
        }
        // Standing up the art fills the hero. Lying down it is on the right, the wide picture takes most of the width and a poster
        // less, and its left edge melts into the page, so the details are over the page and not over the faces
        Box(
            modifier = (if (landscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(if (hasBackdrop) HomeDimens.HeroLandscapeArtFraction else HomeDimens.HeroLandscapePosterFraction)
            } else {
                Modifier.fillMaxWidth()
            }).fillMaxHeight()
        ) {
            // The gradient of the cover stays under the picture, which covers it when it arrives
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(hero.artColors)))
            model?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = if (hasBackdrop) Alignment.Center else Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (landscape) HeroArtEdgeFade()
        }
        // Veil only as tall as the header and a little more, from the top of the screen to just under the numbers, so the
        // texts read and the rest of the art stays clear
        val black = KaisouTheme.colors.veil
        val veilHeight = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding() +
            HomeDimens.HeaderAction + HomeDimens.HeroVeilExtra
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(veilHeight)
                .background(
                    Brush.verticalGradient(
                        0f to black.copy(alpha = HomeDimens.HeroVeilStrength),
                        0.6f to black.copy(alpha = HomeDimens.HeroVeilStrength * 0.6f),
                        1f to Color.Transparent
                    )
                )
        )
        HeroFade(landscape = landscape)

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (landscape) HomeDimens.HeroLandscapeInfoWidth else 1f)
                .padding(horizontal = dimens.screenPadding)
                .padding(bottom = HomeDimens.HeroInfoBottom)
        ) {
            // The logo is asked the way the preload did, so it comes from memory. Without one the name shows in text
            val logo = remember(art, logoSize) { art?.logoUrl?.let { logoRequest(context, it, logoSize) } }
            TitleLogo(
                title = hero.title,
                logo = logo,
                maxHeight = if (landscape) HomeDimens.HeroLandscapeTitleHeight else ComponentDimens.TitleLogoMaxHeight,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.padding(top = if (landscape) dimens.spaceSmall else dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                art?.rating?.let { AgeRatingBadge(it) }
                hero.releaseDay?.let { WeeklyReleaseBadge(it) }
                // Year and format, then a faint divider and the genres, as in 2026 • TV | Action, Mystery
                val details = if (hero.year != null) stringResource(R.string.home_hero_meta, hero.year, hero.format) else hero.format
                val divider = stringResource(R.string.home_hero_meta_divider)
                val dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HomeDimens.MetaDividerAlpha)
                val meta = remember(details, divider, dividerColor, hero.genres) {
                    buildAnnotatedString {
                        append(details)
                        if (hero.genres.isNotBlank()) {
                            withStyle(SpanStyle(color = dividerColor)) { append(divider) }
                            append(hero.genres)
                        }
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = art?.synopsis ?: hero.synopsis,
                style = if (landscape) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                // The buttons stay anchored at the bottom, so a short text just leaves the name lower
                maxLines = if (landscape) HomeDimens.HeroLandscapeSynopsisLines else HomeDimens.HeroSynopsisLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = dimens.spaceSmall)
            )
            Row(
                modifier = Modifier.padding(top = if (landscape) HomeDimens.HeroLandscapeButtonsTop else HomeDimens.HeroButtonsTop),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A movie has no episodes, and the season is only said when it is known
                val season = art?.season
                PrimaryButton(
                    // Lying down the button is not stretched over the whole column
                    modifier = if (landscape) {
                        Modifier.weight(1f, fill = false).widthIn(max = HomeDimens.HeroLandscapeButtonMax)
                    } else {
                        Modifier.weight(1f)
                    },
                    text = when {
                        hero.query.movie -> stringResource(R.string.home_cta_watch_movie)
                        season != null -> stringResource(
                            if (hero.continuing) R.string.home_cta_continue_season else R.string.home_cta_watch_season,
                            season,
                            hero.nextEpisode
                        )
                        else -> stringResource(
                            if (hero.continuing) R.string.home_cta_continue else R.string.home_cta_watch,
                            hero.nextEpisode
                        )
                    },
                    onClick = onPlay,
                    leading = { ButtonIcon(icon = KaisouIcons.PlayOutline, tint = KaisouTheme.colors.onAccentFill) }
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
