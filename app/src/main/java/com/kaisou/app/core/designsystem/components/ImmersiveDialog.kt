package com.kaisou.app.core.designsystem.components

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
// UI utilities
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
// Window of the system
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// A dialog is a window of its own, and it brings the bars of the system back over the app, which is immersive. Called inside the
// content of a dialog, it hides them again in that window, the way the main screen does: they come back for a moment with a swipe
// from the edge, and the dialog gives the window back to the app when it closes
@Composable
fun ImmersiveDialogEffect() {
    val view = LocalView.current

    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
