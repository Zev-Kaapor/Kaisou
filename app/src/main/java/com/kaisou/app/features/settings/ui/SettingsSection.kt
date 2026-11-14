package com.kaisou.app.features.settings.ui

// Android resources and icons
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons

// The names of the screens that open from the sections in the route of the settings
const val ThanksSection = "Thanks"
const val UpdatesSection = "Updates"

// A part of the settings, with the words that name it, the line that says what is in it and its icon
enum class SettingsSection(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector
) {
    About(R.string.settings_about, R.string.settings_about_subtitle, KaisouIcons.Info)
}

// The sections that sit together in a card under a name
enum class SettingsGroup(@StringRes val titleRes: Int, val sections: List<SettingsSection>) {
    More(R.string.settings_group_more, listOf(SettingsSection.About))
}
