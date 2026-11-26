package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.SubcomposeAsyncImage
// Java text
import java.text.NumberFormat
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniViewer
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.designsystem.theme.textShadow
import com.kaisou.app.features.home.ui.HomeDimens

// Logo, the account with what it watched and read, and on the right the notifications, search and the account picture
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHeader(
    viewer: AniViewer?,
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
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)

            // The avatar sits in a bigger touch area, so the edge gets less room to keep the same margin as the logo
            .padding(
                start = dimens.screenPadding,
                end = dimens.screenPadding - (HomeDimens.HeaderAction - HomeDimens.Avatar) / 2,
                top = dimens.spaceExtraSmall,
                bottom = dimens.spaceExtraSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(HomeDimens.Logo)
        )
        Spacer(modifier = Modifier.width(dimens.spaceMedium))
        AccountInfo(viewer = viewer, modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAction(
                icon = KaisouIcons.Bell,
                contentDescription = stringResource(R.string.home_notifications),
                counter = viewer?.unreadNotifications ?: 0,
                onClick = onNotifications
            )
            HeaderAction(
                icon = KaisouIcons.Search,
                contentDescription = stringResource(R.string.home_search),
                onClick = onSearch
            )
            Avatar(
                avatarUrl = viewer?.avatar,
                waiting = viewer == null,
                contentDescription = stringResource(R.string.home_profile),
                onClick = onProfile
            )
        }
    }
}

// Name of the account and, small under it, the episodes watched and the chapters read with their numbers in the accent
@Composable
private fun AccountInfo(viewer: AniViewer?, modifier: Modifier = Modifier) {
    // Until the account is there the name and the numbers are placeholders, in the room they take
    if (viewer == null) {
        SkeletonHost(modifier = modifier) {
            Box(modifier = Modifier.size(HomeDimens.SkeletonNameWidth, HomeDimens.SkeletonNameHeight).skeleton())
            Spacer(modifier = Modifier.height(KaisouTheme.dimens.spaceExtraSmall))
            Box(modifier = Modifier.size(HomeDimens.SkeletonStatsWidth, HomeDimens.SkeletonStatsHeight).skeleton())
        }
        return
    }

    val format = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val episodes = format.format(viewer.episodesWatched)
    val chapters = format.format(viewer.chaptersRead)
    val stats = stringResource(R.string.home_stats, episodes, chapters)
    val description = stringResource(R.string.home_stats_description, episodes, chapters)
    val highlight = KaisouTheme.colors.numberHighlight
    val shadow = textShadow()

    // The numbers are found in the line to take the accent, the rest of the line stays quiet
    val statsText = buildAnnotatedString {
        append(stats)
        val episodesStart = stats.indexOf(episodes)
        if (episodesStart >= 0) {
            addStyle(SpanStyle(color = highlight), episodesStart, episodesStart + episodes.length)
            val chaptersStart = stats.indexOf(chapters, episodesStart + episodes.length)
            if (chaptersStart >= 0) addStyle(SpanStyle(color = highlight), chaptersStart, chaptersStart + chapters.length)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = viewer.name,
            style = MaterialTheme.typography.titleSmall.copy(shadow = shadow),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = statsText,
            style = MaterialTheme.typography.labelSmall.copy(shadow = shadow),
            color = KaisouTheme.colors.onMedia,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { contentDescription = description }
        )
    }
}

// Round button with an icon, the counter of what is new on its corner when asked, and the icon in the accent while active
@Composable
fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    counter: Int = 0,
    active: Boolean = false,
    backdrop: Boolean = false
) {
    Box(
        modifier = modifier
            .size(HomeDimens.HeaderAction)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // The round backdrop is smaller than the touch area, so the button is as easy to touch as the others
        if (backdrop) {
            Box(modifier = Modifier.size(HomeDimens.HeaderBackdrop).background(KaisouTheme.colors.veil.copy(alpha = HomeDimens.HeaderBackdropAlpha), CircleShape))
        }

        // The shadow of the icon is the icon again, dark and blurred, a little lower, doing nothing before Android 12
        val dimens = KaisouTheme.dimens
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KaisouTheme.colors.iconShadow,
            modifier = Modifier
                .size(HomeDimens.HeaderIcon)
                .offset(y = dimens.iconShadowOffset)
                .blur(dimens.iconShadowBlur, BlurredEdgeTreatment.Unbounded)
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else KaisouTheme.colors.onMedia,
            modifier = Modifier.size(HomeDimens.HeaderIcon)
        )
        if (counter > 0) Counter(count = counter, modifier = Modifier.align(Alignment.TopEnd))
    }
}

// Round buttons side by side over a picture on one backdrop in the shape of a pill, as tall as the backdrop of a single button
@Composable
fun HeaderPill(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val color = KaisouTheme.colors.veil.copy(alpha = HomeDimens.HeaderBackdropAlpha)

    Row(
        modifier = modifier.drawBehind {
            val height = HomeDimens.HeaderBackdrop.toPx()
            val inset = (HomeDimens.HeaderAction.toPx() - height) / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, (size.height - height) / 2),
                size = Size(size.width - inset * 2, height),
                cornerRadius = CornerRadius(height / 2)
            )
        },
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// Small round number on the corner of a button, a bigger number than the most it shows reads as that and a plus
@Composable
private fun Counter(count: Int, modifier: Modifier = Modifier) {
    val text = if (count > HomeDimens.CounterMax) "${HomeDimens.CounterMax}+" else count.toString()

    Box(
        modifier = modifier
            .offset(x = -HomeDimens.CounterOffset, y = HomeDimens.CounterOffset)
            .size(HomeDimens.CounterSize)
            .background(MaterialTheme.colorScheme.error, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            maxLines = 1
        )
    }
}

// Round picture of the account: a placeholder while it comes, the person icon when it has no picture or it failed
@Composable
private fun Avatar(
    avatarUrl: String?,
    waiting: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KaisouTheme.colors

    Box(
        modifier = modifier
            .size(HomeDimens.HeaderAction)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(HomeDimens.Avatar)
                .border(
                    width = HomeDimens.AvatarRing,
                    brush = Brush.linearGradient(listOf(colors.avatarRingStart, colors.avatarRingMiddle, colors.avatarRingEnd)),
                    shape = CircleShape
                )
                .padding(HomeDimens.AvatarRing + HomeDimens.AvatarRingGap)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                waiting -> AvatarSkeleton()
                avatarUrl == null -> PersonIcon(contentDescription)
                else -> SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    loading = { AvatarSkeleton() },
                    error = { PersonIcon(contentDescription) }
                )
            }
        }
    }
}

// The person that stands for an account without a picture
@Composable
private fun PersonIcon(contentDescription: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = KaisouIcons.Person,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(HomeDimens.AvatarIcon)
        )
    }
}

// Round placeholder with the light sweeping over it, for the picture that is on the way
@Composable
private fun AvatarSkeleton() {
    SkeletonHost(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().skeleton(CircleShape))
    }
}
