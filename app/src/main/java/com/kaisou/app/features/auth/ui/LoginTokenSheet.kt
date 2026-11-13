package com.kaisou.app.features.auth.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.components.SheetCloseButton
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Sheet from the bottom to sign in with a token of AniList that the user already has: a field to paste it and how to get one.
// What is typed is kept only while the sheet is open, never in the state that survives a rotation
@Composable
fun LoginTokenSheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val dimens = KaisouTheme.dimens
    var token by remember { mutableStateOf("") }

    KaisouSheet(onDismiss = onDismiss) {
        val close = LocalSheetClose.current

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding)
                .padding(bottom = dimens.spaceExtraLarge),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceLarge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.login_token_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                SheetCloseButton()
            }
            Text(
                text = stringResource(R.string.login_token_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // The token is hidden as it is typed or pasted, it is a key to the account
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.login_token_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                shape = MaterialTheme.shapes.medium
            )
            PrimaryButton(
                text = stringResource(R.string.login_token_submit),
                onClick = {
                    onSubmit(token)
                    close()
                },
                enabled = token.isNotBlank()
            )
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                Text(
                    text = stringResource(R.string.login_token_steps_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.login_token_steps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
