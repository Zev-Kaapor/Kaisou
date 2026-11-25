package com.kaisou.app

// Android application
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
// Window styling
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.navigation.KaisouApp
import com.kaisou.app.features.auth.ui.AuthViewModel

// Single activity of the app, immersive with the content under the cutout
class MainActivity : ComponentActivity() {
    // Draws the app edge to edge and handles the return of the login
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent bars with light icons, the app is dark only
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setupImmersiveMode()

        // Handles the login that started the app, once, since a rebuilt activity already did
        if (savedInstanceState == null) handleLoginRedirect(intent)
        setContent {
            KaisouTheme {
                KaisouApp()
            }
        }
    }

    // The browser brings the user back here with the token of AniList
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginRedirect(intent)
    }

    // Hands the address the browser came back with to the login
    private fun handleLoginRedirect(intent: Intent?) {
        ViewModelProvider(this)[AuthViewModel::class.java].handleRedirect(intent?.data)
    }

    // The bars go back into hiding whenever the window gets the focus again
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setupImmersiveMode()
    }

    // Content draws under the cutout and the system bars stay hidden until a swipe
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
