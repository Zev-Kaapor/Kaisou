package com.kaisou.app.core.navigation

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// View model
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.features.auth.ui.AuthState
import com.kaisou.app.features.auth.ui.AuthViewModel
import com.kaisou.app.features.auth.ui.LoginScreen
import com.kaisou.app.features.home.ui.HomeScreen

// The app only opens for a user that is signed in to AniList, the login comes first
@Composable
fun KaisouApp(authViewModel: AuthViewModel = viewModel()) {
    val auth by authViewModel.state.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = auth) {
            // The token of another time is being checked, nothing to show yet
            AuthState.Checking -> Unit
            is AuthState.LoggedOut -> LoginScreen(
                state = state,
                onLogin = { authViewModel.startLogin(context) }
            )
            is AuthState.LoggedIn -> KaisouShell(avatarUrl = state.viewer.avatar)
        }
    }
}

// Shell of the app, the content of the selected tab above the navigation bar
@Composable
private fun KaisouShell(avatarUrl: String?) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The screens draw behind the system bars, each one handles its own insets
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = { KaisouNavBar(selected = tab, onSelect = { tab = it }) }
    ) { innerPadding ->
        when (tab) {
            MainTab.Home -> HomeScreen(
                avatarUrl = avatarUrl,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            )
            else -> TabPlaceholder(
                tab = tab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

// Stand in for the tabs that are not built yet
@Composable
private fun TabPlaceholder(
    tab: MainTab,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.tab_coming_soon, stringResource(tab.label)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
