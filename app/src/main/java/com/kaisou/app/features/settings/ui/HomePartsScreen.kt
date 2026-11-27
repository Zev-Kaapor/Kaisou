package com.kaisou.app.features.settings.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
// Project resources
import com.kaisou.app.R
import com.kaisou.app.features.home.data.HomeLayout
import com.kaisou.app.features.home.data.ShelfId
import com.kaisou.app.features.settings.ui.components.SettingsDivider
import com.kaisou.app.features.settings.ui.components.SettingsGroup
import com.kaisou.app.features.settings.ui.components.SettingsPage
import com.kaisou.app.features.settings.ui.components.SettingsSwitchRow

// The shelves that come from the list of the user, and the ones that come from AniList for everyone, each in a card of its own
private val ListShelves = listOf(
    ShelfId.Watching, ShelfId.Reading, ShelfId.NewEpisodes, ShelfId.PlannedAnime, ShelfId.PlannedManga, ShelfId.CompletedTv, ShelfId.CompletedMovie,
    ShelfId.CompletedManga, ShelfId.PausedAnime, ShelfId.PausedManga, ShelfId.DroppedAnime, ShelfId.DroppedManga, ShelfId.MissingSequels
)
private val DiscoverShelves = listOf(
    ShelfId.RecommendedAnime, ShelfId.RecommendedManga, ShelfId.Trending, ShelfId.TrendingManga, ShelfId.Popular, ShelfId.Upcoming, ShelfId.NewAnime, ShelfId.NewManga
)

// What shows on the Home, as the segments of a video were in Wavvy: each part in a switch with the line that says what it brings
@Composable
fun HomePartsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val layout by HomeLayout.state.collectAsState()

    SettingsPage(title = stringResource(R.string.settings_home_parts_title), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_home_stories)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_home_stories_row),
                subtitle = stringResource(R.string.settings_home_stories_row_subtitle),
                checked = layout.shown(HomeLayout.Stories),
                onCheckedChange = { HomeLayout.setShown(HomeLayout.Stories, it) }
            )
        }
        SettingsGroup(title = stringResource(R.string.settings_home_lists)) {
            ShelfSwitches(ListShelves)
        }
        SettingsGroup(title = stringResource(R.string.settings_home_discover)) {
            ShelfSwitches(DiscoverShelves)
        }
    }
}

// A switch for each shelf, with its name and what is on it, a line between two of them
@Composable
private fun ShelfSwitches(shelves: List<ShelfId>) {
    val layout by HomeLayout.state.collectAsState()

    shelves.forEachIndexed { index, shelf ->
        if (index > 0) SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(shelf.titleRes),
            subtitle = stringResource(shelf.descRes),
            checked = layout.shown(shelf.name),
            onCheckedChange = { HomeLayout.setShown(shelf.name, it) }
        )
    }
}
