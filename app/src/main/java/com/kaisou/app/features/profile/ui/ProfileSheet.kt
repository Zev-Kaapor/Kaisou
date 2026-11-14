package com.kaisou.app.features.profile.ui

// Compose animation
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
// Kotlin utilities and coroutines
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniViewer
import com.kaisou.app.core.designsystem.components.clickableNoIndication
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.profile.ui.components.ProfileAvatar

// Menu of the account that rises from the bottom over the app, closed by the dimmed area, the back button or dragging down
@Composable
fun ProfileSheet(
    visible: Boolean,
    viewer: AniViewer,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(KaisouMotion.SheetMillis)),
            exit = fadeOut(tween(KaisouMotion.SheetMillis))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ProfileDimens.ScrimAlpha))
                    .clickableNoIndication(onClick = onDismiss)
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            enter = slideInVertically(tween(KaisouMotion.SheetMillis)) { it },
            exit = slideOutVertically(tween(KaisouMotion.SheetMillis)) { it }
        ) {
            SheetContent(
                viewer = viewer,
                onDismiss = onDismiss,
                onSignOut = onSignOut,
                onProfile = onProfile,
                onSettings = onSettings
            )
        }
    }
}

// Surface of the menu with the account on top, a line and the list below
@Composable
private fun SheetContent(
    viewer: AniViewer,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit
) {
    val dimens = KaisouTheme.dimens
    val scope = rememberCoroutineScope()
    val dismissDistance = with(LocalDensity.current) { ProfileDimens.DismissDistance.toPx() }
    val dragged = remember { Animatable(0f) }
    val shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

    Column(
        modifier = Modifier
            .widthIn(max = ProfileDimens.MenuMaxWidth)
            .fillMaxWidth()
            .offset { IntOffset(0, dragged.value.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    scope.launch { dragged.snapTo((dragged.value + delta).coerceAtLeast(0f)) }
                },
                onDragStopped = {
                    if (dragged.value > dismissDistance) onDismiss() else dragged.animateTo(0f)
                }
            )
            .clip(shape)
            .background(KaisouTheme.colors.elevated)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(bottom = dimens.spaceLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Handle that shows the menu can be dragged
        Box(
            modifier = Modifier
                .padding(vertical = dimens.spaceMedium)
                .size(width = ProfileDimens.HandleWidth, height = ProfileDimens.HandleHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )

        AccountHeader(viewer = viewer, onSignOut = onSignOut)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
            thickness = dimens.hairline,
            color = MaterialTheme.colorScheme.outline
        )

        MenuItem(icon = KaisouIcons.Person, label = stringResource(R.string.profile_your_profile), onClick = onProfile)
        MenuItem(icon = KaisouIcons.Settings, label = stringResource(R.string.profile_settings), onClick = onSettings)
    }
}

// Photo, name and the service the account is of, with the sign out icon at the end
@Composable
private fun AccountHeader(
    viewer: AniViewer,
    onSignOut: () -> Unit
) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        ProfileAvatar(photo = viewer.avatar, size = ProfileDimens.MenuAvatar)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewer.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.profile_provider),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Sign out in red
        Box(
            modifier = Modifier
                .clickableNoIndication(onClick = onSignOut)
                .minimumInteractiveComponentSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = KaisouIcons.SignOut,
                contentDescription = stringResource(R.string.cd_sign_out),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(ProfileDimens.ActionIcon)
            )
        }
    }
}

// Row of the list with an icon and a name
@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileDimens.ItemHeight)
            .clickableNoIndication(onClick = onClick)
            .padding(horizontal = dimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceLarge)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(ProfileDimens.ItemIcon)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
