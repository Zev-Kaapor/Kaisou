package com.kaisou.app.features.title.ui.components

// Android framework
import android.text.format.DateFormat
// Compose animation
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// Image loading
import coil3.compose.AsyncImage
// Java time and utilities
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.rememberScoreFormat
import com.kaisou.app.features.title.data.EpisodeGroups
import com.kaisou.app.features.title.data.EpisodeSources
import com.kaisou.app.features.title.data.ListStatus
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.TitleEpisode
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.data.upNext
import com.kaisou.app.features.title.ui.TitleDimens

// The tab of the episodes of an anime or the chapters of a manga, as Dantotsu has it, with source, list, card, ranges and list
fun LazyListScope.titleEpisodesItems(
    title: TitleUi,
    episodes: Load<List<TitleEpisode>>,
    group: Int?,
    newestFirst: Boolean,
    onGroup: (Int) -> Unit,
    onSort: (Boolean) -> Unit,
    onMark: (Int) -> Unit,
    sourceId: String?,
    onSource: (String?) -> Unit,
    onRetry: () -> Unit
) {
    if (title.manga) {
        item(key = "chapters-soon") { ChaptersSoon(title) }
        return
    }
    if (EpisodeSources.Enabled) item(key = "episodes-source") { SourceBlock(sourceId, onSource) }
    when (episodes) {
        is Load.Done -> {
            val groups = EpisodeGroups.of(episodes.value)
            if (groups.isEmpty()) {
                item(key = "episodes-empty") { EpisodesMessage(R.string.title_episodes_empty) }
                return
            }

            // What the user already did: all of it when the title is done, up to the last one done when it is going
            val watched = when (title.listStatus) {
                ListStatus.Completed -> Int.MAX_VALUE
                ListStatus.Current, ListStatus.Repeating, ListStatus.Paused, ListStatus.Dropped -> title.nextUnit - 1
                else -> 0
            }

            // The one that comes next, only for a title being done, and whether the user can mark progress, which needs the list
            val next = title.upNext
            val canMark = title.entry != null
            val selected = EpisodeGroups.open(groups, group, next)
            val list = if (newestFirst) groups[selected].episodes.asReversed() else groups[selected].episodes
            val total = episodes.value.size
            val done = watched.coerceAtMost(total)
            val upNext = episodes.value.firstOrNull { it.number == next }

            item(key = "episodes-heading") { EpisodesHeading(done, total, newestFirst, onSort) }
            if (EpisodeSources.Enabled && upNext != null) {
                item(key = "episodes-continue") {
                    val soon = stringResource(R.string.title_play_soon)
                    ContinueCard(upNext, done.toFloat() / total) { if (sourceId == null && EpisodeSources.installed.isNotEmpty()) showSources(sourceId, onSource) else Toasts.show(soon) }
                }
            }
            if (groups.size > 1) item(key = "episodes-groups") { GroupChips(groups, selected, onGroup) }
            items(list, key = { "episode-${it.number}" }) { episode ->
                EpisodeRow(
                    episode = episode,
                    watched = episode.number <= watched,
                    isNext = episode.number == next,
                    onMark = if (canMark) onMark else null
                )
            }
        }
        is Load.Failed -> item(key = "episodes-error") { DetailsError(onRetry = onRetry) }
        else -> item(key = "episodes-loading") { EpisodesSkeleton() }
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

// The message of the episodes tab
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

// An episode, plain: its picture and number on the left, its name, score, day and length on the right, with touches to open and mark
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeRow(episode: TitleEpisode, watched: Boolean, isNext: Boolean, onMark: ((Int) -> Unit)?) {
    val dimens = KaisouTheme.dimens
    val haptic = LocalHapticFeedback.current
    val name = episode.title ?: stringResource(R.string.title_episode_untitled, episode.number)
    val canOpen = episode.overview != null
    var open by rememberSaveable(episode.number) { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (open) TitleDimens.ChevronOpen else 0f, tween(KaisouMotion.ExpandMillis), label = "episodeChevron")
    val fade by animateFloatAsState(if (watched) TitleDimens.WatchedAlpha else 1f, tween(KaisouMotion.ExpandMillis), label = "episodeFade")
    val titleColor by animateColorAsState(
        if (watched) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
        tween(KaisouMotion.ExpandMillis),
        label = "episodeTitle"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onLongClickLabel = onMark?.let { stringResource(if (watched) R.string.title_episode_unmark else R.string.title_episode_mark) },
                onLongClick = onMark?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it(if (watched) episode.number - 1 else episode.number)
                    }
                },
                onClick = { if (canOpen) open = !open }
            )
            .padding(horizontal = dimens.screenPadding)
            .padding(top = TitleDimens.EpisodeTop)
            .animateContentSize(tween(KaisouMotion.ExpandMillis))
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(TitleDimens.EpisodeThumb)
                    .aspectRatio(TitleDimens.EpisodeRatio)
                    .clip(MaterialTheme.shapes.medium)
                    .background(KaisouTheme.colors.skeleton)
                    .then(if (isNext) Modifier.border(TitleDimens.EpisodeNextBorder, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                episode.image?.let {
                    AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().alpha(fade))
                }

                // The number on a tab stuck to the corner of the picture, in the accent when the episode comes next
                Text(
                    text = episode.number.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isNext) KaisouTheme.colors.onAccentFill else MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(bottomEnd = TitleDimens.EpisodeTabCorner))
                        .background(if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
                        .padding(horizontal = dimens.spaceMedium, vertical = TitleDimens.EpisodeTabVertical)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                if (isNext) {
                    Text(text = stringResource(R.string.title_episodes_next), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = titleColor)
                EpisodeMeta(episode)
            }
            if (canOpen) {
                Icon(
                    imageVector = KaisouIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(TitleDimens.EpisodeChevron).rotate(chevron)
                )
            }
        }
        if (open) {
            episode.overview?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = dimens.spaceMedium))
            }
        }
    }
}

// The line under the name of an episode: its score with a star, the day it aired and how long it is
@Composable
private fun EpisodeMeta(episode: TitleEpisode) {
    val dimens = KaisouTheme.dimens
    val format = rememberScoreFormat()
    val separator = stringResource(R.string.home_card_separator)
    val duration = episode.minutes?.let { stringResource(R.string.title_duration_minutes, it) }
    val day = remember(episode.aired) {
        episode.aired?.let {
            runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMd"), Locale.getDefault())) }.getOrNull()
        }
    }
    val rest = listOfNotNull(day, duration).joinToString(separator)

    if (episode.rating == null && rest.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
        episode.rating?.let { rating ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = KaisouIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.EpisodeStar))
                Text(text = format.format(rating), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        if (rest.isNotEmpty()) Text(text = rest, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// The placeholder of the list: the controls and some rows, each with the picture and three bars, as the ones that will come
@Composable
private fun EpisodesSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceMedium)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(TitleDimens.SkeletonSortWidth, TitleDimens.SkeletonSortHeight).skeleton(CircleShape))
            Box(modifier = Modifier.size(TitleDimens.SkeletonSortWidth, TitleDimens.SkeletonSortHeight).skeleton(CircleShape))
        }
        repeat(TitleDimens.EpisodeSkeletonRows) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.EpisodeTop), horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)) {
                Box(modifier = Modifier.width(TitleDimens.EpisodeThumb).aspectRatio(TitleDimens.EpisodeRatio).skeleton(MaterialTheme.shapes.medium))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    Box(modifier = Modifier.fillMaxWidth(EpisodeLineLong).height(ComponentDimens.SkeletonLineHeight).skeleton())
                    Box(modifier = Modifier.fillMaxWidth(EpisodeLineShort).height(ComponentDimens.SkeletonLineHeight).skeleton())
                }
            }
        }
    }
}

// How long the long and the short placeholder lines of an episode are
private const val EpisodeLineLong = 0.8f
private const val EpisodeLineShort = 0.4f
