package com.kaisou.app.features.user.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
// Project resources
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.user.ui.UserDimens

// The placeholder of the page of a person in the places of what comes: banner, name, follow button, lines of text and three cards
@Composable
internal fun UserSkeleton(bannerTop: Dp, showFollow: Boolean) {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(UserDimens.BannerHeight + bannerTop).skeleton(MaterialTheme.shapes.extraSmall)) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(bottom = dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(UserDimens.Avatar).background(KaisouTheme.colors.skeletonHighlight, CircleShape))

                // The name, the year they joined and the follow counts, in the places of the lines of the real header
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonName).height(dimens.spaceLarge).background(KaisouTheme.colors.skeletonHighlight, MaterialTheme.shapes.extraSmall))
                        }
                        if (showFollow) Box(modifier = Modifier.size(UserDimens.FollowSkeletonWidth, UserDimens.FollowSize).background(KaisouTheme.colors.skeletonHighlight, MaterialTheme.shapes.small))
                    }
                    Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonSince).height(ComponentDimens.SkeletonLineHeight).background(KaisouTheme.colors.skeletonHighlight, MaterialTheme.shapes.extraSmall))
                    Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonCounts).height(ComponentDimens.SkeletonLineHeight).background(KaisouTheme.colors.skeletonHighlight, MaterialTheme.shapes.extraSmall))
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
            repeat(UserDimens.SkeletonAboutLines) { index ->
                Box(modifier = Modifier.fillMaxWidth(if (index == UserDimens.SkeletonAboutLines - 1) UserDimens.SkeletonLastLine else 1f).height(ComponentDimens.SkeletonLineHeight).skeleton())
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
            repeat(2) { Box(modifier = Modifier.fillMaxWidth().height(UserDimens.SkeletonTotals).skeleton(MaterialTheme.shapes.medium)) }
        }
        repeat(2) { index ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
                Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonTitle).height(dimens.spaceLarge).skeleton())
                Box(modifier = Modifier.padding(top = dimens.spaceSmall).fillMaxWidth().height(if (index == 0) UserDimens.SkeletonGenres else UserDimens.SkeletonDays).skeleton(MaterialTheme.shapes.medium))
            }
        }
    }
}

// The placeholder of the last things they did, which come after the page: the title and some cards
@Composable
internal fun UserRecentSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonTitle).height(dimens.spaceLarge).skeleton())
        Column(modifier = Modifier.padding(top = dimens.spaceSmall), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
            repeat(UserDimens.SkeletonRecent) { ActivityCardSkeleton(actions = false) }
        }
    }
}

// A card of what a person did, in the place of what it has: the cover, two lines and, with the buttons, the actions at the end
@Composable
internal fun ActivityCardSkeleton(actions: Boolean) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(KaisouTheme.colors.elevated).padding(dimens.spaceSmall)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(UserDimens.RecentCover).aspectRatio(HomeDimens.PosterRatio).skeleton(MaterialTheme.shapes.small))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                Box(modifier = Modifier.fillMaxWidth().height(ComponentDimens.SkeletonLineHeight).skeleton())
                Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonSince).height(ComponentDimens.SkeletonLineHeight).skeleton())
            }
        }
        if (actions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(UserDimens.SkeletonButtons) { Box(modifier = Modifier.size(TitleDimens.ThreadAction).padding(dimens.spaceExtraSmall).skeleton(CircleShape)) }
            }
        }
    }
}

// The placeholder of the page with all they did: some cards in the places of the real ones, with the buttons
@Composable
internal fun UserActivitySkeleton(cards: Int) {
    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceSmall)) {
            repeat(cards) { ActivityCardSkeleton(actions = true) }
        }
    }
}
