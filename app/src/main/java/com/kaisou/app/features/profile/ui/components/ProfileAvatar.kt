package com.kaisou.app.features.profile.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
// Image loading
import coil3.compose.SubcomposeAsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.profile.ui.ProfileDimens

// Round avatar with the gradient ring, the photo of the account or the person icon when there is none
@Composable
fun ProfileAvatar(
    photo: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val colors = KaisouTheme.colors
    val ring = Brush.linearGradient(
        colors = listOf(colors.avatarRingStart, colors.avatarRingMiddle, colors.avatarRingEnd),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    val description = stringResource(R.string.cd_profile)
    val person: @Composable () -> Unit = {
        Icon(
            imageVector = KaisouIcons.Person,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(size * ProfileDimens.AvatarIconFraction)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .border(ProfileDimens.AvatarRing, ring, CircleShape)
            .padding(ProfileDimens.AvatarRing + ProfileDimens.AvatarRingGap)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            SubcomposeAsyncImage(
                model = photo,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { person() }
            )
        } else {
            person()
        }
    }
}
