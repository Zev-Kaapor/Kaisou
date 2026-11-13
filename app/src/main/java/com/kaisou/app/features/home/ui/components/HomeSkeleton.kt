package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
// Project resources
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.SkeletonLines
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Placeholder of the Home while AniList has not answered: each block is where the logo, the details, the synopsis, the
// buttons and the row of the friends go, so what comes next does not move anything
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val density = LocalDensity.current
    val width = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }

    Column(modifier = modifier.fillMaxWidth()) {
        // The art of the hero with the details anchored to its bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(width * HomeDimens.HeroAspect)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SkeletonHost(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPadding)
                    .padding(bottom = HomeDimens.HeroInfoBottom)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeDimens.HeroLogoHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ComponentDimens.TitleSkeletonWidthFraction)
                            .height(ComponentDimens.TitleSkeletonHeight)
                            .skeleton()
                    )
                }
                Row(
                    modifier = Modifier.padding(top = dimens.spaceMedium),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = HomeDimens.SkeletonTagWidth, height = ComponentDimens.RatingBadge)
                            .skeleton()
                    )
                    Box(
                        modifier = Modifier
                            .size(width = HomeDimens.SkeletonMetaWidth, height = ComponentDimens.SkeletonLineHeight)
                            .skeleton()
                    )
                }
                SkeletonLines(
                    lines = HomeDimens.HeroSynopsisLines,
                    modifier = Modifier.padding(top = dimens.spaceSmall)
                )
                Row(
                    modifier = Modifier.padding(top = HomeDimens.HeroButtonsTop),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeight)
                            .skeleton(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(ComponentDimens.IconButtonLarge)
                            .skeleton(CircleShape)
                    )
                }
            }
        }

        // The row of the friends with its title and the cards under it
        SkeletonHost(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(HomeDimens.SectionSpacing))
            Box(
                modifier = Modifier
                    .padding(horizontal = dimens.screenPadding)
                    .size(width = HomeDimens.SkeletonShelfTitleWidth, height = HomeDimens.SkeletonShelfTitleHeight)
                    .skeleton()
            )
            Spacer(modifier = Modifier.height(dimens.spaceMedium))
            // The cards keep their width and run past the edge of the screen, like the row of the real shelf
            Row(
                modifier = Modifier
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .padding(horizontal = dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                repeat(HomeDimens.SkeletonCards) {
                    Box(
                        modifier = Modifier
                            .size(ComponentDimens.StoryWidth, ComponentDimens.StoryHeight)
                            .skeleton(MaterialTheme.shapes.large)
                    )
                }
            }
        }
    }
}
