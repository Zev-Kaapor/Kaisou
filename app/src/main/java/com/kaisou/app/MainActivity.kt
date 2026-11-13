package com.kaisou.app

// Android framework
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
// Activity and Compose host
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// View model
import androidx.lifecycle.ViewModelProvider
// Window insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Project resources
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.navigation.KaisouApp
import com.kaisou.app.features.auth.ui.AuthViewModel
import com.kaisou.app.features.home.ui.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AniListSession.init(this)
        // Transparent bars with light icons, the app is dark only
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // Immersive mode, a swipe from the edge brings the bars back for a moment
        WindowCompat.getInsetsController(window, window.decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // The login can start the app, when it was closed while the user was in the browser
        handleLoginRedirect(intent)
        // For a user that is already signed in the Home starts loading now, before the first frame, and the screen finds it ready
        if (AniListSession.token != null) ViewModelProvider(this)[HomeViewModel::class.java]
        setContent {
            KaisouTheme {
                KaisouApp()
            }
        }
        hideSystemBars()
    }

    // The browser brings the user back here with the token of AniList
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginRedirect(intent)
    }

    private fun handleLoginRedirect(intent: Intent?) {
        ViewModelProvider(this)[AuthViewModel::class.java].handleRedirect(intent?.data)
    }

    // The bars go back into hiding whenever the window gets the focus again
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
    }
}
