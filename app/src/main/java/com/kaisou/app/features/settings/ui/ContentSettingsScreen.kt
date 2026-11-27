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
import com.kaisou.app.features.settings.data.AdultContent
import com.kaisou.app.features.settings.data.AdultMode
import com.kaisou.app.features.settings.ui.components.SettingsDivider
import com.kaisou.app.features.settings.ui.components.SettingsGroup
import com.kaisou.app.features.settings.ui.components.SettingsPage
import com.kaisou.app.features.settings.ui.components.SettingsSwitchRow

// What the user wants of adult content: only it, or none of it, the two never on together, and both off showing everything
@Composable
fun ContentSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val mode by AdultContent.mode.collectAsState()

    SettingsPage(title = stringResource(R.string.settings_content), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_content_adult)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_adult_only),
                subtitle = stringResource(R.string.settings_adult_only_subtitle),
                checked = mode == AdultMode.Only,
                onCheckedChange = AdultContent::setOnly
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_adult_hide),
                subtitle = stringResource(R.string.settings_adult_hide_subtitle),
                checked = mode == AdultMode.Hide,
                onCheckedChange = AdultContent::setHide
            )
        }
    }
}
