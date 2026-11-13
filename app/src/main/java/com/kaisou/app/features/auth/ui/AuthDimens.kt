package com.kaisou.app.features.auth.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes that only the login uses
object AuthDimens {
    val LogoSize = 112.dp
    // Bunch of keys on the button of the token
    val KeyMark = 24.dp
    // Icon of AniList inside its badge, it has room around the drawing
    val AniListMark = 26.dp
    // Widest the buttons and their text get, so they do not stretch on big screens
    val ActionsMaxWidth = 360.dp
    // Space under the hint in portrait, so it does not sit on the edge of the screen
    val BottomSpace = 24.dp
}
