package com.kaisou.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Bar docked at the bottom with the main destinations, icons together in the middle. When a screen puts its sections in it, the items of the
// destinations go up and out and the ones of the sections come up from below, and the other way when the screen goes
@Composable
fun KaisouNavBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    sections: NavSections?,
    modifier: Modifier = Modifier
) {
    // The sections of the last screen, kept to go on drawing them while they are going out
    val kept = remember { arrayOfNulls<NavSections>(1) }
    if (sections != null) kept[0] = sections

    NavBarContainer(modifier = modifier) {
        AnimatedContent(
            targetState = sections != null,
            modifier = Modifier.weight(1f),
            transitionSpec = { navSwap() },
            label = "NavBarSwap"
        ) { showSections ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val own = kept[0]
                if (showSections && own != null) {
                    own.labels.forEachIndexed { index, label ->
                        NavBarItem(
                            icon = own.icons[index],
                            label = label,
                            selected = index == own.selected,
                            onClick = { NavSectionsHost.select(index) },
                            modifier = Modifier.weight(1f).heightIn(min = NavBarDimens.Height)
                        )
                    }
                } else {
                    MainTab.entries.forEach { tab ->
                        NavBarItem(
                            icon = tab.icon,
                            label = stringResource(tab.label),
                            selected = tab == selected,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.weight(1f).heightIn(min = NavBarDimens.Height)
                        )
                    }
                }
            }
        }
    }
}

// The movement of the items of the bar and of the rail when they are swapped: going to the sections the old ones go up and out and the new
// ones come from below, coming back it is all the other way
internal fun AnimatedContentTransitionScope<Boolean>.navSwap(): ContentTransform {
    val toSections = targetState
    val move = tween<IntOffset>(KaisouMotion.ExpandMillis)
    val fade = tween<Float>(KaisouMotion.ExpandMillis)
    return (slideInVertically(move) { if (toSections) it else -it } + fadeIn(fade)) togetherWith
        (slideOutVertically(move) { if (toSections) -it else it } + fadeOut(fade))
}

// The body of the bar, docked at the bottom over its own color, with the items together in the middle. The bar of the main destinations
// and the one of the sections of a title are both drawn in it
@Composable
internal fun NavBarContainer(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val dimens = KaisouTheme.dimens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(KaisouTheme.colors.navBar)
            // Grows when the system bars come back with a swipe
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = NavBarDimens.GroupMaxWidth)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// Icon and name, the selected one takes the accent color and grows a little, the name can be hidden
@Composable
internal fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val colors = KaisouTheme.colors
    val tint by animateColorAsState(
        targetValue = if (selected) colors.navSelected else colors.navUnselected,
        animationSpec = tween(durationMillis = KaisouMotion.IconScaleMillis),
        label = "NavBarItemTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else KaisouMotion.UnselectedIconScale,
        animationSpec = tween(durationMillis = KaisouMotion.IconScaleMillis),
        label = "NavBarItemScale"
    )

    Column(
        modifier = modifier.selectable(
            selected = selected,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (showLabel) null else label,
            tint = tint,
            modifier = Modifier
                .size(NavBarDimens.IconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )

        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
