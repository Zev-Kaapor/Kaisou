package com.kaisou.app.features.auth.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.theme.AniListNight
import com.kaisou.app.features.auth.ui.AuthDimens

// Icon of AniList on a dark circle, so the white letter of the icon shows over the violet button
@Composable
fun AniListBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ComponentDimens.ButtonLeading)
            .background(AniListNight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_anilist),
            contentDescription = null,
            modifier = Modifier.size(AuthDimens.AniListMark)
        )
    }
}
