package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.model.ContentRating

// Advisory rating: the Brazilian symbol, or the rating of MyAnimeList written in a small outlined tag
@Composable
fun RatingBadge(
    rating: ContentRating,
    modifier: Modifier = Modifier,
    size: Dp = ComponentDimens.RatingBadge
) {
    when (rating) {
        is ContentRating.Brazilian -> Image(
            painter = painterResource(rating.rating.icon),
            contentDescription = stringResource(rating.rating.description),
            modifier = modifier.size(size)
        )
        is ContentRating.Mal -> {
            val description = stringResource(rating.rating.description)
            val tone = MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = modifier
                    .height(size)
                    .border(ComponentDimens.RatingTagBorder, tone, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = KaisouTheme.dimens.spaceExtraSmall)
                    .clearAndSetSemantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(rating.rating.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = tone,
                    maxLines = 1
                )
            }
        }
    }
}

// Title of a shelf
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KaisouTheme.dimens.screenPadding)
    )
}
