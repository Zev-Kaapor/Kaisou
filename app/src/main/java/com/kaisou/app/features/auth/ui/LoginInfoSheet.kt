package com.kaisou.app.features.auth.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.SheetCloseButton
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Sheet from the bottom that explains the login in full: what happens to the password, what the app can do with the
// account, where the key stays and how to take the access back
@Composable
fun LoginInfoSheet(onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens

    KaisouSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding)
                .padding(bottom = dimens.spaceExtraLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraLarge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.login_info_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                SheetCloseButton()
            }
            InfoItem(R.string.login_info_password_title, R.string.login_info_password)
            InfoItem(R.string.login_info_access_title, R.string.login_info_access)
            InfoItem(R.string.login_info_storage_title, R.string.login_info_storage)
            InfoItem(R.string.login_info_revoke_title, R.string.login_info_revoke)
        }
    }
}

// A title and the paragraph under it
@Composable
private fun InfoItem(title: Int, text: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
