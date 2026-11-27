package com.kaisou.app.features.settings.ui

// UI styling and utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
// Project resources
import com.kaisou.app.R
import com.kaisou.app.features.settings.ui.components.SettingsGroup
import com.kaisou.app.features.settings.ui.components.SettingsNavigateRow
import com.kaisou.app.features.settings.ui.components.SettingsPage

// The settings of the Home, where the button to show or hide its parts opens the screen with a switch for each one
@Composable
fun HomeSettingsScreen(onBack: () -> Unit, onParts: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPage(title = stringResource(R.string.settings_home), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_home_components)) {
            SettingsNavigateRow(
                title = stringResource(R.string.settings_home_parts),
                subtitle = stringResource(R.string.settings_home_parts_subtitle),
                onClick = onParts
            )
        }
    }
}
