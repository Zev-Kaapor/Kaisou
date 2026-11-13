package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Image loading
import coil3.compose.AsyncImage
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
// Project resources
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Logo on the left and the notifications, search and profile on the right, over the hero until it scrolls away
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHeader(
    avatarUrl: String?,
    containerColor: Color,
    onNotifications: () -> Unit,
    onSearch: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            // The status bar is hidden, its space stays so the header keeps clear of the camera
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(start = dimens.screenPadding, end = dimens.spaceExtraSmall, top = dimens.spaceExtraSmall, bottom = dimens.spaceExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(HomeDimens.Logo)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAction(
                icon = KaisouIcons.Bell,
                contentDescription = stringResource(R.string.home_notifications),
                onClick = onNotifications
            )
            HeaderAction(
                icon = KaisouIcons.Search,
                contentDescription = stringResource(R.string.home_search),
                onClick = onSearch
            )
            ProfileAvatar(
                avatarUrl = avatarUrl,
                contentDescription = stringResource(R.string.home_profile),
                onClick = onProfile
            )
        }
    }
}

// Plain icon on the header, no background
@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(HomeDimens.HeaderAction)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = KaisouTheme.colors.onMedia,
            modifier = Modifier.size(HomeDimens.HeaderIcon)
        )
    }
}

// Round profile button, the picture of the account and a person icon while it is not there or when the account has none
@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(HomeDimens.HeaderAction)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(HomeDimens.Avatar)
                .border(KaisouTheme.dimens.spaceMicro, MaterialTheme.colorScheme.onBackground, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = KaisouIcons.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(HomeDimens.AvatarIcon)
            )
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
