package com.kaisou.app.features.auth.ui

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.PrimaryButton
import com.kaisou.app.core.designsystem.components.QuietButton
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.designsystem.theme.backgroundGlow
import com.kaisou.app.features.auth.ui.components.AniListBadge
import com.kaisou.app.features.auth.ui.components.KeyMark

// Names of the links inside the texts, the explanation of the login and the page to create an account
private const val LearnMoreTag = "learn_more"
private const val SignUpTag = "sign_up"

// The first screen: the brand and the way in, since the app needs an AniList account. While the saved login is being
// checked only the brand shows, in the same place, so the buttons do not push it when they come
@Composable
fun LoginScreen(
    checking: Boolean,
    busy: Boolean,
    error: Int?,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onTokenLogin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    var tokenOpen by rememberSaveable { mutableStateOf(false) }

    // The sheet is drawn in this same window, over everything, so the immersive mode stays
    Box(modifier = modifier.fillMaxSize()) {
        LoginContent(
            checking = checking,
            busy = busy,
            error = error,
            onLogin = onLogin,
            onSignUp = onSignUp,
            onLearnMore = { infoOpen = true },
            onToken = { tokenOpen = true }
        )
        if (infoOpen) LoginInfoSheet(onDismiss = { infoOpen = false })
        if (tokenOpen) LoginTokenSheet(onDismiss = { tokenOpen = false }, onSubmit = onTokenLogin)
    }
}

// The brand and the way in, side by side lying down and one above the other standing up
@Composable
private fun LoginContent(
    checking: Boolean,
    busy: Boolean,
    error: Int?,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onLearnMore: () -> Unit,
    onToken: () -> Unit
) {
    val dimens = KaisouTheme.dimens

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .backgroundGlow()
            // The camera cutout and the bars, when they are showing, are left clear
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (maxWidth > maxHeight) {
            // Lying down the brand and the way in sit side by side
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Brand(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight())
                Actions(
                    checking = checking,
                    busy = busy,
                    error = error,
                    onLogin = onLogin,
                    onSignUp = onSignUp,
                    onLearnMore = onLearnMore,
                    onToken = onToken,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.screenPadding)
                    .padding(bottom = AuthDimens.BottomSpace),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The brand fills the space above the buttons
                Brand(modifier = Modifier.weight(1f))
                Actions(
                    checking = checking,
                    busy = busy,
                    error = error,
                    onLogin = onLogin,
                    onSignUp = onSignUp,
                    onLearnMore = onLearnMore,
                    onToken = onToken
                )
            }
        }
    }
}

// Logo, name and what the app is
@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceMedium, Alignment.CenterVertically)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(AuthDimens.LogoSize)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// What went wrong the last time, what the login does with the password and the two ways in
@Composable
private fun Actions(
    checking: Boolean,
    busy: Boolean,
    error: Int?,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onLearnMore: () -> Unit,
    onToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))

    Column(
        modifier = modifier
            .widthIn(max = AuthDimens.ActionsMaxWidth)
            .fillMaxWidth()
            // Held in place but out of sight while the saved login is checked
            .alpha(if (checking) 0f else 1f)
            .then(if (checking) Modifier.clearAndSetSemantics {} else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        error?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        // What the login does with the credentials in one sentence, the rest is one tap away on the link that ends it
        val privacy = stringResource(R.string.login_privacy)
        val learnMore = stringResource(R.string.login_learn_more)
        Text(
            text = buildAnnotatedString {
                append("$privacy ")
                withLink(LinkAnnotation.Clickable(tag = LearnMoreTag, styles = linkStyle) { onLearnMore() }) {
                    append(learnMore)
                }
                append(".")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        PrimaryButton(
            text = stringResource(if (busy) R.string.login_loading else R.string.login_button),
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            leading = { AniListBadge() },
            loading = busy,
            enabled = !checking && !busy
        )
        QuietButton(
            text = stringResource(R.string.login_token_button),
            onClick = onToken,
            modifier = Modifier.fillMaxWidth(),
            leading = { KeyMark() },
            enabled = !checking && !busy
        )
        // The way to get an account is a line of its own, not a third button
        val noAccount = stringResource(R.string.login_no_account)
        val signUp = stringResource(R.string.login_signup)
        Text(
            text = buildAnnotatedString {
                append("$noAccount ")
                withLink(LinkAnnotation.Clickable(tag = SignUpTag, styles = linkStyle) { onSignUp() }) {
                    append(signUp)
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
