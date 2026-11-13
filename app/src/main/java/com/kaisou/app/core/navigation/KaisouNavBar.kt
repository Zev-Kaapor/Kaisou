package com.kaisou.app.core.navigation

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Bottom bar with an icon over the name of each destination, the selected one in the accent
@OptIn(ExperimentalLayoutApi::class)
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
                NavItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val dimens = KaisouTheme.dimens
    val tint = if (selected) MaterialTheme.colorScheme.primary else KaisouTheme.colors.navUnselected

    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = NavBarDimens.Height)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(dimens.iconSize)
        )
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
