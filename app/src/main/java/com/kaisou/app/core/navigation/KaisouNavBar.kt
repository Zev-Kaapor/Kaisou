package com.kaisou.app.core.navigation

// Compose animation and layouts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Bar docked at the bottom with the main destinations, icons together in the middle
@Composable
fun KaisouNavBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                NavBarItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = NavBarDimens.Height)
                )
            }
        }
    }
}

// Icon and name, the selected one takes the accent color and grows a little, the name can be hidden
@Composable
internal fun NavBarItem(
    tab: MainTab,
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
            imageVector = tab.icon,
            contentDescription = if (showLabel) null else stringResource(tab.label),
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
                text = stringResource(tab.label),
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
