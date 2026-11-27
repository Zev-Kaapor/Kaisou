package com.kaisou.app.features.user.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.KaisouSwitch
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.user.data.ListSort

// The icon each way of ordering has on its tile
private val SortIcons: Map<ListSort, ImageVector> = mapOf(
    ListSort.Title to KaisouIcons.SortAlphabetical,
    ListSort.Score to KaisouIcons.Star,
    ListSort.Progress to KaisouIcons.ListChecks,
    ListSort.Updated to KaisouIcons.Clock,
    ListSort.Added to KaisouIcons.ListPlus,
    ListSort.Started to KaisouIcons.PlayOutline,
    ListSort.Completed to KaisouIcons.Check,
    ListSort.Release to KaisouIcons.Calendar,
    ListSort.Weighted to KaisouIcons.BarChart,
    ListSort.Popularity to KaisouIcons.Person
)

// The sheet that orders a list: a card that turns the order round, the ways to order as tiles two to a line, cancel and apply
@Composable
fun ListSortSheet(sort: ListSort, reversed: Boolean, onApply: (ListSort, Boolean) -> Unit, onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens
    var pickedSort by remember { mutableStateOf(sort) }
    var pickedReversed by remember { mutableStateOf(reversed) }

    KaisouSheet(onDismiss = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        val closeSheet = LocalSheetClose.current

        // The name of the sheet
        Row(
            modifier = Modifier.fillMaxWidth().height(UserDimens.FilterHeader).padding(horizontal = dimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.user_sort_sheet), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }

        // The card of the way it goes and the tiles of the ways to order
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            val goesUp = pickedSort.goesUp(pickedReversed)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(KaisouTheme.colors.elevated)
                    .clickable(role = Role.Switch) { pickedReversed = !pickedReversed }
                    .padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (goesUp) KaisouIcons.SortAscending else KaisouIcons.SortDescending,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UserDimens.SortGlyph)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.user_sort_reverse), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = stringResource(if (goesUp) R.string.user_sort_up else R.string.user_sort_down),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                KaisouSwitch(checked = pickedReversed, onCheckedChange = null)
            }

            // The ways to order, two to a line, the picked one lit
            ListSort.entries.chunked(2).forEach { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                    line.forEach { way -> SortTile(way, picked = way == pickedSort) { pickedSort = way } }
                    if (line.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Cancel closes it as it was, apply keeps what was chosen
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceSmall, bottom = dimens.spaceMedium),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            FilterButton(text = stringResource(R.string.action_cancel), filled = false, onClick = closeSheet, modifier = Modifier.weight(1f))
            FilterButton(
                text = stringResource(R.string.user_filter_apply),
                filled = true,
                onClick = {
                    onApply(pickedSort, pickedReversed)
                    closeSheet()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// A way to order as a tile with its icon and name, filled with the accent when it is the one picked
@Composable
private fun RowScope.SortTile(way: ListSort, picked: Boolean, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val content = if (picked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .weight(1f)
            .height(UserDimens.SortTile)
            .clip(MaterialTheme.shapes.large)
            .background(if (picked) MaterialTheme.colorScheme.primaryContainer else KaisouTheme.colors.elevated)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = dimens.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = SortIcons.getValue(way), contentDescription = null, tint = if (picked) content else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(UserDimens.SortGlyph))
        Text(
            text = stringResource(way.label),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (picked) FontWeight.Bold else FontWeight.Medium),
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
