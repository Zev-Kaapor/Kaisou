package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.title.data.ListStatus
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.TitleEpisode
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.ui.TitleDimens

// The tab of the episodes of an anime, or of the chapters of a manga. A manga only has how many it has, the chapters themselves come with
// the sources of reading. The anime has its episodes, each with its picture, its name, how long it is and when it aired, the ones the user
// already did are dimmed with a mark, and the order can be turned
fun LazyListScope.titleEpisodesItems(
    title: TitleUi,
    episodes: Load<List<TitleEpisode>>,
    newestFirst: Boolean,
    onToggleOrder: () -> Unit,
    onRetry: () -> Unit
) {
    if (title.manga) {
        item(key = "chapters-soon") { ChaptersSoon(title) }
        return
    }
    when (episodes) {
        is Load.Done -> {
            val list = if (newestFirst) episodes.value.asReversed() else episodes.value
            if (list.isEmpty()) {
                item(key = "episodes-empty") { EpisodesMessage(R.string.title_episodes_empty) }
                return
            }
            item(key = "episodes-order") { OrderToggle(newestFirst = newestFirst, onToggle = onToggleOrder) }
            // What the user already did: all of it when the title is done, up to the last one done when it is going
            val watched = when (title.listStatus) {
                ListStatus.Completed -> Int.MAX_VALUE
                ListStatus.Current, ListStatus.Repeating, ListStatus.Paused, ListStatus.Dropped -> title.nextUnit - 1
                else -> 0
            }
            items(list, key = { "episode-${it.number}" }) { episode -> EpisodeRow(episode = episode, watched = episode.number <= watched) }
        }
        is Load.Failed -> item(key = "episodes-error") { DetailsError(onRetry = onRetry) }
        else -> item(key = "episodes-loading") { DetailsSkeleton() }
    }
}

// What the tab of a manga says: how many chapters and volumes there are, and that the list comes with the sources
@Composable
private fun ChaptersSoon(title: TitleUi) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = Modifier.fillMaxWidth().padding(dimens.screenPadding).padding(top = TitleDimens.BlockTop),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val counts = listOfNotNull(
            title.chapters?.let { pluralStringResource(R.plurals.home_card_chapters, it, it) },
            title.volumes?.let { pluralStringResource(R.plurals.title_volumes, it, it) }
        ).joinToString(stringResource(R.string.home_card_separator))
        if (counts.isNotEmpty()) {
            Text(text = counts, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = stringResource(R.string.title_chapters_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EpisodesMessage(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(KaisouTheme.dimens.screenPadding).padding(top = TitleDimens.BlockTop)
    )
}

// The link that turns the order of the list, in the accent, on the right
@Composable
private fun OrderToggle(newestFirst: Boolean, onToggle: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceMedium), horizontalArrangement = Arrangement.End) {
        Text(
            text = stringResource(if (newestFirst) R.string.title_episodes_sort_new else R.string.title_episodes_sort_old),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(role = Role.Button, onClick = onToggle)
        )
    }
}

// An episode: its picture on the left with a mark when the user already did it, and on the right its number and name, how long it is and
// the day it aired
@Composable
private fun EpisodeRow(episode: TitleEpisode, watched: Boolean) {
    val dimens = KaisouTheme.dimens
    val separator = stringResource(R.string.home_card_separator)
    val name = episode.title ?: stringResource(R.string.title_episode_untitled, episode.number)
    val details = listOfNotNull(episode.minutes?.let { stringResource(R.string.title_duration_minutes, it) }, episode.aired).joinToString(separator)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.EpisodeTop),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        Box(
            modifier = Modifier
                .width(TitleDimens.EpisodeThumb)
                .aspectRatio(TitleDimens.EpisodeRatio)
                .clip(MaterialTheme.shapes.medium)
                .background(KaisouTheme.colors.skeleton),
            contentAlignment = Alignment.Center
        ) {
            episode.image?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().alpha(if (watched) TitleDimens.WatchedAlpha else 1f)
                )
            }
            if (watched) {
                Box(
                    modifier = Modifier.size(TitleDimens.WatchedMark).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = KaisouIcons.Check,
                        contentDescription = null,
                        tint = KaisouTheme.colors.onAccentFill,
                        modifier = Modifier.size(TitleDimens.WatchedMarkIcon)
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
            Text(
                text = stringResource(R.string.title_episode_row, episode.number, name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
