package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Avatar with the initial of the user, ringed with a gradient while the story was not seen
@Composable
private fun StoryAvatar(
    initial: String,
    artColors: List<Color>,
    imageUrl: String?,
    seen: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = KaisouTheme.colors
    val ring = if (seen) {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
    } else {
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, colors.storyRingEnd))
    }

    Box(
        modifier = modifier
            .size(ComponentDimens.StoryAvatar)
            .border(ComponentDimens.StoryRing, ring, CircleShape)
            .padding(ComponentDimens.StoryRing)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ArtImage(artColors, imageUrl, Modifier.matchParentSize())
        // The initial stands for the photo only when the user has none
        if (imageUrl == null) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onMedia
            )
        }
    }
}

// What a friend did, over the cover of the title: who, and a short line about the activity
@Composable
fun StoryCard(
    name: String,
    activity: String,
    initial: String,
    coverUrl: String?,
    avatarUrl: String?,
    coverColors: List<Color>,
    avatarColors: List<Color>,
    seen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors

    Box(
        modifier = modifier
            .size(ComponentDimens.StoryWidth, ComponentDimens.StoryHeight)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        ArtImage(coverColors, coverUrl, Modifier.matchParentSize())
        // Dark fade under the text
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.scrimStrong)))
        )
        StoryAvatar(
            initial = initial,
            artColors = avatarColors,
            imageUrl = avatarUrl,
            seen = seen,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(dimens.spaceSmall)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(dimens.spaceSmall)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onMedia,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = activity,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onMedia,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// First card of the row, opens the place to post a status
@Composable
fun OwnStoryCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .size(ComponentDimens.StoryWidth, ComponentDimens.StoryHeight)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(dimens.spaceSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(ComponentDimens.IconButton)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = KaisouIcons.Plus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        )
    }
}
