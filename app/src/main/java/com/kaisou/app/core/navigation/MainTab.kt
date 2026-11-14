package com.kaisou.app.core.navigation

// Android resources
import androidx.annotation.StringRes
// UI styling and utilities
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons

// Main destinations shown in the navigation bar, in this order
enum class MainTab(
    val route: String,
    val icon: ImageVector,
    @StringRes val label: Int
) {
    Home("home", KaisouIcons.Home, R.string.nav_home),
    Explore("explore", KaisouIcons.Compass, R.string.nav_explore),
    Calendar("calendar", KaisouIcons.Calendar, R.string.nav_calendar),
    Library("library", KaisouIcons.Library, R.string.nav_library)
}
