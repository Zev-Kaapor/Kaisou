package com.kaisou.app.features.auth.ui

// Compose layouts and foundations
import android.content.ClipboardManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.components.QuietButton
import com.kaisou.app.core.designsystem.components.SheetCloseButton
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Sheet from the bottom to sign in with a token of AniList: a field to paste it and how to get one. When the page of AniList that
// shows the token was opened in the browser, the clipboard is looked at when the user comes back and the token that was made for this
// app is used by itself, and a button pastes it by hand otherwise. What is typed is kept only while the sheet is open, never in the
// state that survives a rotation
@Composable
fun LoginTokenSheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    byBrowser: Boolean,
    isPinToken: (String) -> Boolean
) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    var token by remember { mutableStateOf("") }
    var away by remember { mutableStateOf(false) }

    // What was copied, which the system only lets the app read while it is in front
    val copied: () -> String? = {
        context.getSystemService(ClipboardManager::class.java)
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
            ?.trim()
    }

    KaisouSheet(onDismiss = onDismiss) {
        val close = LocalSheetClose.current

        // The window loses focus when the browser opens, and when it has it back the token is looked for. A token that was already in
        // the clipboard before the user left is never used, so signing out and in again is not instant
        LaunchedEffect(focused) {
            if (!byBrowser) return@LaunchedEffect
            if (!focused) {
                away = true
            } else if (away) {
                copied()?.takeIf(isPinToken)?.let {
                    onSubmit(it)
                    close()
                }
            }
        }

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
                text = stringResource(if (byBrowser) R.string.login_token_description_browser else R.string.login_token_description),
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
            if (byBrowser) {
                QuietButton(
                    text = stringResource(R.string.login_token_paste),
                    onClick = { copied()?.let { token = it } }
                )
            }
            PrimaryButton(
                text = stringResource(R.string.login_token_submit),
                onClick = {
                    onSubmit(token)
                    close()
                },
                enabled = token.isNotBlank()
            )
            // Without the page of AniList that shows the token, the user makes the client and the token by hand
            if (!byBrowser) {
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
}
