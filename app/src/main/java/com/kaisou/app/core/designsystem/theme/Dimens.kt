package com.kaisou.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
// UI styling and utilities
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Window widths, in dp, where the layout moves to the next size
private const val MediumWidthStart = 600
private const val ExpandedWidthStart = 840

// How wide the window is
enum class WindowWidth { Compact, Medium, Expanded }

// Spacing on a 4dp grid, plus the values that grow with the window width
@Immutable
data class Dimens(
    val spaceExtraSmall: Dp = 4.dp,
    val spaceSmall: Dp = 8.dp,
    val spaceMedium: Dp = 12.dp,
    val spaceLarge: Dp = 16.dp,
    val spaceExtraLarge: Dp = 24.dp,
    // Thin line between rows of a card and under the header of a menu
    val hairline: Dp = 0.5.dp,
    // Soft shadow of the texts over pictures: how far it falls and how much it spreads
    val textShadowOffset: Dp = 1.dp,
    val textShadowBlur: Dp = 6.dp,
    // Soft shadow of the icons over pictures, drawn twice, a blurred one behind and the icon: how far it falls and spreads
    val iconShadowOffset: Dp = 1.dp,
    val iconShadowBlur: Dp = 3.dp,
    // Height of the pill buttons
    val buttonHeight: Dp = 48.dp,
    // Margin at the sides of the screen
    val screenPadding: Dp
)

// Window width from the width of the screen in dp
fun windowWidthFor(screenWidthDp: Int): WindowWidth = when {
    screenWidthDp >= ExpandedWidthStart -> WindowWidth.Expanded
    screenWidthDp >= MediumWidthStart -> WindowWidth.Medium
    else -> WindowWidth.Compact
}

// Tokens for each window width
fun dimensFor(windowWidth: WindowWidth): Dimens = when (windowWidth) {
    WindowWidth.Compact -> Dimens(screenPadding = 16.dp)
    WindowWidth.Medium -> Dimens(screenPadding = 24.dp)
    WindowWidth.Expanded -> Dimens(screenPadding = 32.dp)
}

// Tokens of the current window width
val LocalDimens = staticCompositionLocalOf { dimensFor(WindowWidth.Compact) }
