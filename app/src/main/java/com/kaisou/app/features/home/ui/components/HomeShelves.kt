package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.EpisodeCard
import com.kaisou.app.core.designsystem.components.OwnStoryCard
import com.kaisou.app.core.designsystem.components.PosterCard
import com.kaisou.app.core.designsystem.components.SectionHeader
import com.kaisou.app.core.designsystem.components.StoryCard
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.model.ContentRating
import com.kaisou.app.features.home.data.EpisodeUi
import com.kaisou.app.features.home.data.PosterRatings
import com.kaisou.app.features.home.data.PosterUi
import com.kaisou.app.features.home.data.StoryUi

// Title and a row that scrolls sideways, the items of the row are up to the caller
@Composable
private fun Shelf(
    title: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            content = content
        )
    }
}

// Latest activity of the friends, the first card opens the place to post a status
@Composable
fun StoriesShelf(
    stories: List<StoryUi>,
    onOpenStory: (StoryUi) -> Unit,
    onPostStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ownLabel = stringResource(R.string.home_your_status)

    Shelf(
        title = stringResource(R.string.home_stories_title),
        modifier = modifier
    ) {
        item {
            OwnStoryCard(
                label = ownLabel,
                onClick = onPostStatus
            )
        }
        items(stories) { story ->
            StoryCard(
                name = story.name,
                activity = story.activity,
                initial = story.initial,
                coverUrl = story.coverUrl,
                avatarUrl = story.avatarUrl,
                coverColors = story.coverColors,
                avatarColors = story.avatarColors,
                seen = story.seen,
                onClick = { onOpenStory(story) }
            )
        }
    }
}

@Composable
fun NewEpisodesShelf(
    items: List<EpisodeUi>,
    onOpen: (EpisodeUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Shelf(
        title = stringResource(R.string.home_new_episodes_title),
        modifier = modifier
    ) {
        items(items) { item ->
            EpisodeCard(
                title = item.title,
                details = item.details,
                tag = item.tag,
                artColors = item.artColors,
                imageUrl = item.imageUrl,
                onClick = { onOpen(item) }
            )
        }
    }
}

@Composable
fun PosterShelf(
    title: String,
    items: List<PosterUi>,
    ratings: PosterRatings,
    onOpen: (PosterUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Shelf(
        title = title,
        modifier = modifier
    ) {
        items(items) { item ->
            // The rating comes after the shelf, each card asks for the one of its title while it is on the screen
            val rating by produceState<ContentRating?>(ratings.known(item), item.anilistId) {
                if (value == null) value = ratings.load(item)
            }
            PosterCard(
                title = item.title,
                details = item.details,
                rating = rating,
                artColors = item.artColors,
                imageUrl = item.imageUrl,
                onClick = { onOpen(item) },
                onMore = {}
            )
        }
    }
}
