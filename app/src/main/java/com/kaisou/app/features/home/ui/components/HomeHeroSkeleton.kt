package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
// Project resources
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.SkeletonLines
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens

// Placeholder of the hero, each block where the real one goes so nothing moves, with one light sweeping over all of it
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHeroSkeleton(
    height: Dp,
    landscape: Boolean,
    modifier: Modifier = Modifier
) {
    val art = MaterialTheme.colorScheme.surfaceContainer

    SkeletonHost(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (landscape) {
                        Modifier
                    } else {
                        // The fade is in the transparency of the block, so the light only shows where the block is
                        Modifier.background(
                            Brush.verticalGradient(
                                0f to art,
                                HomeDimens.HeroFadeStart to art,
                                HomeDimens.HeroFadeEnd to Color.Transparent
                            )
                        )
                    }
                )
        ) {
            // The art is what is above the details and under the header, with the icon of an image in its middle
            val headerHeight = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding() + HomeDimens.HeaderAction
            if (landscape) {
                // The art block on the right, with its left edge and its bottom melting into the page like the real art
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(HomeDimens.HeroLandscapeArtFraction)
                        .fillMaxHeight()
                        .meltingArt(art)
                )
                ArtIcon(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(HomeDimens.HeroLandscapeArtFraction)
                        .fillMaxHeight()
                        .padding(top = headerHeight)
                )
                SkeletonDetails(
                    landscape = true,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(HomeDimens.HeroLandscapeInfoWidth)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ArtIcon(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = headerHeight)
                    )
                    SkeletonDetails(landscape = false, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// The art lying down as a block that melts into the page by turning transparent, so the sweep shows only where the art is
@Composable
private fun Modifier.meltingArt(color: Color): Modifier {
    val bottom = HomeDimens.HeroLandscapeBottomFade
    val left = remember {
        val stops = HomeDimens.HeroArtEdgeFadeStops.map { (at, cover) -> at * HomeDimens.HeroArtEdgeFadeReach to Color.Black.copy(alpha = 1f - cover) }
        Brush.horizontalGradient(*stops.toTypedArray(), 1f to Color.Black)
    }
    val down = remember {
        Brush.verticalGradient(
            1f - bottom to Color.Black,
            1f - bottom * 0.6f to Color.Black.copy(alpha = 0.65f),
            1f - bottom * 0.25f to Color.Black.copy(alpha = 0.2f),
            1f to Color.Transparent
        )
    }

    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .background(color)
        .drawWithContent {
            drawContent()
            drawRect(brush = left, blendMode = BlendMode.DstIn)
            drawRect(brush = down, blendMode = BlendMode.DstIn)
        }
}

// The icon of an image in the middle of the room it is given
@Composable
private fun ArtIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = KaisouIcons.Image,
            contentDescription = null,
            tint = KaisouTheme.colors.skeleton,
            modifier = Modifier.size(HomeDimens.SkeletonArtIcon)
        )
    }
}

// The details anchored to the bottom: the title, the tags and the line of details, the synopsis and the buttons
@Composable
private fun SkeletonDetails(landscape: Boolean, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .padding(horizontal = dimens.screenPadding)
            .padding(bottom = HomeDimens.HeroInfoBottom)
    ) {
        // Room of the title, with the shape of a name in it
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (landscape) HomeDimens.HeroLandscapeTitleHeight else HomeDimens.HeroTitleHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(HomeDimens.SkeletonTitleWidthFraction)
                    .height(HomeDimens.SkeletonTitleHeight)
                    .skeleton()
            )
        }

        // The tag of the day and the line with the year, the format and the genres
        Row(
            modifier = Modifier.padding(top = if (landscape) dimens.spaceSmall else dimens.spaceMedium),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(HomeDimens.SkeletonTagWidth, HomeDimens.HeroTagHeight).skeleton())
            Box(modifier = Modifier.size(HomeDimens.SkeletonMetaWidth, ComponentDimens.SkeletonLineHeight).skeleton())
        }
        SkeletonLines(
            lines = if (landscape) HomeDimens.HeroLandscapeSynopsisLines else HomeDimens.HeroSynopsisLines,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        )

        // The main button and the round one next to it
        Row(
            modifier = Modifier.padding(top = if (landscape) HomeDimens.HeroLandscapeButtonsTop else HomeDimens.HeroButtonsTop),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = (if (landscape) Modifier.weight(1f, fill = false).widthIn(max = HomeDimens.HeroLandscapeButtonMax) else Modifier.weight(1f))
                    .fillMaxWidth()
                    .height(dimens.buttonHeight)
                    .skeleton(CircleShape)
            )
            Box(modifier = Modifier.size(ComponentDimens.IconButton).skeleton(CircleShape))
        }
    }
}

// The fade of the art into the page, the same as the real art: down when standing up, only the bottom when lying down
@Composable
fun HeroFade(landscape: Boolean, modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val bottom = HomeDimens.HeroLandscapeBottomFade

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (landscape) {
                    // Eased, so there is no band where the art ends
                    Brush.verticalGradient(
                        1f - bottom to Color.Transparent,
                        1f - bottom * 0.6f to background.copy(alpha = 0.35f),
                        1f - bottom * 0.25f to background.copy(alpha = 0.8f),
                        1f to background
                    )
                } else {
                    Brush.verticalGradient(
                        HomeDimens.HeroFadeStart to Color.Transparent,
                        HomeDimens.HeroFadeEnd to background
                    )
                }
            )
    )
}

// The left edge of the art lying down melting into the page over a long eased stretch, with no dark band
@Composable
fun HeroArtEdgeFade(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background

    // The brush is made once for a color, not at each frame of the screen
    val brush = remember(background) {
        val stops = HomeDimens.HeroArtEdgeFadeStops.map { (at, alpha) -> at * HomeDimens.HeroArtEdgeFadeReach to background.copy(alpha = alpha) }
        Brush.horizontalGradient(*stops.toTypedArray(), 1f to Color.Transparent)
    }

    Box(modifier = modifier.fillMaxSize().background(brush))
}
