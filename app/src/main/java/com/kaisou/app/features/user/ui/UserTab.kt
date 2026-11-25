package com.kaisou.app.features.user.ui

// Android resources
import androidx.annotation.StringRes
// UI styling and utilities
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons

// The sections of the page of a person, which the bar of the app shows: overview, lists, statistics and community
enum class UserTab(@StringRes val label: Int, val icon: ImageVector) {
    Overview(R.string.user_tab_overview, KaisouIcons.Person),
    Lists(R.string.user_tab_lists, KaisouIcons.Library),
    Stats(R.string.user_tab_stats, KaisouIcons.BarChart),
    Community(R.string.user_tab_community, KaisouIcons.Comment)
}
