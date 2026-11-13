package com.kaisou.app.features.auth.ui

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// The only way in: the app needs the AniList account, so there is nothing to see before it
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    state: AuthState.LoggedOut,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
            .padding(horizontal = dimens.screenPadding)
            .padding(bottom = AuthDimens.BottomSpace),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The brand fills the space above the button
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium, Alignment.CenterVertically)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(AuthDimens.LogoSize)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // What went wrong the last time, if anything
        state.error?.let { error ->
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = dimens.spaceMedium)
            )
        }
        PrimaryButton(
            text = stringResource(if (state.busy) R.string.login_loading else R.string.login_button),
            icon = KaisouIcons.LogIn,
            onClick = { if (!state.busy) onLogin() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(dimens.spaceMedium))
        Text(
            text = stringResource(R.string.login_hint),
            style = MaterialTheme.typography.labelMedium,
            color = KaisouTheme.colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}
