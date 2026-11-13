package com.kaisou.app.core.navigation

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
// View model
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.features.auth.ui.AuthState
import com.kaisou.app.features.auth.ui.AuthViewModel
import com.kaisou.app.features.auth.ui.LoginScreen

// The app only opens for a user that is signed in to AniList, the login comes first
@Composable
fun KaisouApp(authViewModel: AuthViewModel = viewModel()) {
    val auth by authViewModel.state.collectAsState()
    val context = LocalContext.current

    when (val state = auth) {
        // The login of another time is being checked, the screen shows only the brand
        AuthState.Checking -> LoginScreen(
            checking = true,
            busy = false,
            error = null,
            onLogin = {},
            onSignUp = {},
            onTokenLogin = {}
        )
        is AuthState.LoggedOut -> LoginScreen(
            checking = false,
            busy = state.busy,
            error = state.error,
            onLogin = { authViewModel.startLogin(context) },
            onSignUp = { authViewModel.startSignUp(context) },
            onTokenLogin = authViewModel::signInWithToken
        )
        is AuthState.LoggedIn -> KaisouShell()
    }
}

// The tab that is selected, with the navigation bar below it, or the rail on the side in landscape
@Composable
private fun KaisouShell() {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                KaisouNavRail(selected = tab, onSelect = { tab = it })

                // The rail already makes room for the camera cutout on its side, the content keeps away from it on the other
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                ) {
                    TabContent(tab = tab, modifier = Modifier.fillMaxSize())
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TabContent(tab = tab, modifier = Modifier.weight(1f))
                KaisouNavBar(selected = tab, onSelect = { tab = it })
            }
        }
    }
}

// Where the screen of the selected tab goes, the screens arrive one at a time
@Composable
private fun TabContent(tab: MainTab, modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}
