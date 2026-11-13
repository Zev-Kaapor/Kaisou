package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.model.ContentRating
// Image loading
import coil3.compose.AsyncImage

// Gradient that stands in for artwork until the real images are loaded
@Composable
fun ArtPlaceholder(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Brush.linearGradient(colors)))
}

// The image of a title over the gradient, which shows while the image is not there and stays if there is none
@Composable
fun ArtImage(
    colors: List<Color>,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ArtPlaceholder(colors = colors, modifier = Modifier.matchParentSize())
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

// Portrait cover with the title below and a line with the rating, the details and the menu of the title
@Composable
fun PosterCard(
    title: String,
    details: String,
    rating: ContentRating?,
    artColors: List<Color>,
    imageUrl: String?,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .width(ComponentDimens.PosterWidth)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        ArtImage(
            colors = artColors,
            imageUrl = imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ComponentDimens.PosterAspect)
                .clip(MaterialTheme.shapes.extraSmall)
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rating != null) {
                    RatingBadge(rating, size = ComponentDimens.PosterRatingBadge)
                    Spacer(Modifier.width(dimens.spaceSmall))
                }
                Text(
                    text = if (rating != null) stringResource(R.string.poster_details_with_rating, details) else details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = KaisouIcons.MoreVertical,
                    contentDescription = stringResource(R.string.action_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(dimens.iconSize)
                        .clickable(onClick = onMore)
                )
            }
        }
    }
}

// Landscape thumbnail with the title and a line of details below, the content of the thumbnail is up to the caller
@Composable
private fun LandscapeCard(
    title: String,
    details: String,
    artColors: List<Color>,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: @Composable BoxScope.() -> Unit
) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .width(ComponentDimens.LandscapeWidth)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ComponentDimens.LandscapeAspect)
                .clip(MaterialTheme.shapes.extraSmall)
        ) {
            ArtImage(artColors, imageUrl, Modifier.matchParentSize())
            thumbnail()
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// An episode that just aired, with a tag in the corner
@Composable
fun EpisodeCard(
    title: String,
    details: String,
    tag: String,
    artColors: List<Color>,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    LandscapeCard(
        title = title,
        details = details,
        artColors = artColors,
        imageUrl = imageUrl,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(dimens.spaceSmall)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceMicro)
        )
    }
}
