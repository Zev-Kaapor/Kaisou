package com.kaisou.app.features.auth.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.features.auth.ui.AuthDimens

// Bunch of keys in the colors of the brand, at the start of the button that signs in with a token
@Composable
fun KeyMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(ComponentDimens.ButtonLeading),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_login_key),
            contentDescription = null,
            modifier = Modifier.size(AuthDimens.KeyMark)
        )
    }
}
