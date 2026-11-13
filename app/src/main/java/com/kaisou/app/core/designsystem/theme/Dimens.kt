package com.kaisou.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
// UI styling and utilities
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Spacing and sizes shared by every screen
@Immutable
data class Dimens(
    val spaceMicro: Dp = 2.dp,
    val spaceExtraSmall: Dp = 4.dp,
    val spaceSmall: Dp = 8.dp,
    val spaceMedium: Dp = 12.dp,
    val spaceExtraLarge: Dp = 24.dp,
    val buttonHeight: Dp = 48.dp,
    val iconSize: Dp = 24.dp,
    val screenPadding: Dp = 16.dp
)

val LocalDimens = staticCompositionLocalOf { Dimens() }
