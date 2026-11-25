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

// Hides the bars of the system again inside a dialog, which brings them back, the way the main screen does
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
