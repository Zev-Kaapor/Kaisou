package com.kaisou.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.AnimatedContent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Rail on the left with the icons of the destinations stacked, used when the screen is wider than tall, swapping for sections
@Composable
fun KaisouNavRail(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    sections: NavSections?,
    modifier: Modifier = Modifier
) {
    val kept = remember { arrayOfNulls<NavSections>(1) }
    if (sections != null) kept[0] = sections

    // The camera cutout takes the first part of the rail, so the icons never go under it
    val cutout = WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
    val iconsInset = maxOf(cutout - NavBarDimens.RailCameraReduction, NavBarDimens.NoInset)
    val railWidth = if (cutout > NavBarDimens.NoInset) {
        NavBarDimens.RailIconAreaWidth + iconsInset
    } else {
        NavBarDimens.RailDefaultWidth
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .background(KaisouTheme.colors.navBar),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = sections != null,
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = iconsInset)
                .width(NavBarDimens.RailIconAreaWidth),
            transitionSpec = { navSwap() },
            label = "NavRailSwap"
        ) { showSections ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NavBarDimens.RailItemSpacing, Alignment.CenterVertically)
            ) {
                val own = kept[0]
                if (showSections && own != null) {
                    own.labels.forEachIndexed { index, label ->
                        NavBarItem(
                            icon = own.icons[index],
                            label = label,
                            selected = index == own.selected,
                            onClick = { NavSectionsHost.select(index) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = NavBarDimens.RailItemHeight),
                            showLabel = false
                        )
                    }
                } else {
                    MainTab.entries.forEach { tab ->
                        NavBarItem(
                            icon = tab.icon,
                            label = stringResource(tab.label),
                            selected = tab == selected,
                            onClick = { onSelect(tab) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = NavBarDimens.RailItemHeight),
                            showLabel = false
                        )
                    }
                }
            }
        }
    }
}
