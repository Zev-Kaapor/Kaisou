package com.kaisou.app.features.user.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Dates
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
// Images
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.features.home.ui.components.CardRating
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.ui.components.TitleCard
import com.kaisou.app.features.home.ui.components.rememberCardRating
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.components.SeeAllArrow
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.BlockTitle
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.home.ui.components.rememberScoreFormat
import com.kaisou.app.features.user.data.ListSort
import com.kaisou.app.features.user.data.ListTabs
import com.kaisou.app.features.user.data.UserListEntry
import com.kaisou.app.features.user.data.UserListSection
import com.kaisou.app.features.user.ui.UserDimens
import com.kaisou.app.features.user.ui.UserListsUi

// The lists of a person: a card per kind with its states, what they are watching and reading, and what they finished lately
fun LazyListScope.userListsItems(
    state: UserListsUi,
    onOpenTitle: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpenList: (manga: Boolean, tab: String) -> Unit
) {
    if (state.anime is Load.Failed && state.manga is Load.Failed) {
        item(key = "lists-error") { DetailsError(onRetry = onRetry) }
        return
    }
    item(key = "lists-cards") {
        val dimens = KaisouTheme.dimens
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            KindCard(manga = false, load = state.anime, onClick = { onOpenList(false, ListTabs.PageOrder.first()) })
            KindCard(manga = true, load = state.manga, onClick = { onOpenList(true, ListTabs.PageOrder.first()) })
        }
    }

    // The kinds that came, the anime first
    val kinds = listOf(false to (state.anime as? Load.Done)?.value, true to (state.manga as? Load.Done)?.value).mapNotNull { (manga, sections) -> sections?.let { manga to it } }
    kinds.forEach { (manga, sections) ->
        val current = ListTabs.entries(sections, "CURRENT").sortedWith(ListSort.Updated.comparator())
        if (current.isNotEmpty()) item(key = "lists-current-$manga") {
            CardsRowBlock(
                title = stringResource(if (manga) R.string.user_row_reading else R.string.user_row_watching),
                entries = current.take(UserDimens.ListWatching),
                line = EntryLine.Progress,
                onOpen = { onOpenList(manga, "CURRENT") },
                onOpenTitle = onOpenTitle
            )
        }
    }

    // The other states, each a row with the ones touched last, the anime first and then the manga
    val others = listOf(
        Triple("PAUSED", EntryLine.Progress, R.string.user_block_paused_anime to R.string.user_block_paused_manga),
        Triple("DROPPED", EntryLine.Progress, R.string.user_block_dropped_anime to R.string.user_block_dropped_manga),
        Triple("PLANNING", EntryLine.Facts, R.string.user_block_planning_anime to R.string.user_block_planning_manga)
    )
    others.forEach { (status, line, titles) ->
        kinds.forEach { (manga, sections) ->
            val entries = ListTabs.entries(sections, status).sortedWith(ListSort.Updated.comparator())
            if (entries.isNotEmpty()) item(key = "lists-${status.lowercase()}-$manga") {
                CardsRowBlock(
                    title = stringResource(if (manga) titles.second else titles.first),
                    entries = entries.take(UserDimens.ListWatching),
                    line = line,
                    onOpen = { onOpenList(manga, status) },
                    onOpenTitle = onOpenTitle
                )
            }
        }
    }

    // What they finished lately closes the page, the anime first and then the manga
    kinds.forEach { (manga, sections) ->
        val finished = ListTabs.entries(sections, "COMPLETED").sortedByDescending { it.finishedDate() }
        if (finished.isNotEmpty()) item(key = "lists-finished-$manga") {
            CardsRowBlock(
                title = stringResource(if (manga) R.string.user_block_finished_manga else R.string.user_block_finished_anime),
                entries = finished.take(UserDimens.ListFinished),
                line = EntryLine.Day,
                onOpen = { onOpenList(manga, "COMPLETED") },
                onOpenTitle = onOpenTitle
            )
        }
    }
    if (state.anime is Load.Loading || state.manga is Load.Loading) item(key = "lists-loading") { ListRowsSkeleton() }
}

// The switch between the anime and the manga: two halves of a pill, the one that is open filled with the accent
@Composable
internal fun KindSwitch(manga: Boolean, onPick: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    Row(modifier = modifier.clip(CircleShape).background(KaisouTheme.colors.elevated).padding(UserDimens.SwitchPadding)) {
        listOf(false to R.string.profile_type_anime, true to R.string.profile_type_manga).forEach { (isManga, label) ->
            val selected = manga == isManga
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) KaisouTheme.colors.onAccentFill else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(role = Role.Tab) { onPick(isManga) }
                    .padding(vertical = dimens.spaceSmall)
            )
        }
    }
}

// The card of a kind: its name and count, a bar split in the states and each state with its color and count
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KindCard(manga: Boolean, load: Load<List<UserListSection>>, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val palette = KaisouTheme.colors.chart
    val keys = stringArrayResource(R.array.title_list_keys)
    val names = stringArrayResource(if (manga) R.array.title_list_names_manga else R.array.title_list_names_anime)
    val sections = (load as? Load.Done)?.value
    val counts = remember(sections) { ListTabs.StatusOrder.map { status -> status to (sections?.firstOrNull { !it.custom && it.key == status }?.entries?.size ?: 0) }.filter { it.second > 0 } }
    val total = counts.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .clickable(enabled = sections != null, role = Role.Button, onClick = onClick)
            .padding(dimens.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (manga) R.string.user_list_manga else R.string.user_list_anime),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (sections != null) {
                    Text(text = pluralStringResource(R.plurals.profile_titles_count, total, total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (load !is Load.Failed) {
                    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceExtraSmall)) {
                        Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonSince).height(ComponentDimens.SkeletonLineHeight).skeleton())
                    }
                }
            }

            // The arrow says that the card opens the page of this list
            Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            sections == null && load is Load.Failed -> Text(text = stringResource(R.string.user_list_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            sections == null -> SkeletonHost(modifier = Modifier.fillMaxWidth()) {
                // The bar and two lines of states, as tall as the card is once the list comes
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                    Box(modifier = Modifier.fillMaxWidth().height(UserDimens.GenreBarHeight).skeleton(MaterialTheme.shapes.small))
                    Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonCounts).height(ComponentDimens.SkeletonLineHeight).skeleton())
                    Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonSince).height(ComponentDimens.SkeletonLineHeight).skeleton())
                }
            }
            total == 0 -> Text(text = stringResource(R.string.user_list_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                Row(modifier = Modifier.fillMaxWidth().height(UserDimens.GenreBarHeight).clip(MaterialTheme.shapes.small)) {
                    counts.forEach { (status, count) ->
                        Box(modifier = Modifier.weight(count.toFloat()).fillMaxHeight().background(palette[ListTabs.StatusOrder.indexOf(status) % palette.size]))
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    counts.forEach { (status, count) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(UserDimens.ListDot).background(palette[ListTabs.StatusOrder.indexOf(status) % palette.size], CircleShape))
                            Text(
                                text = "${names.getOrElse(keys.indexOf(status)) { status }} $count",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// The name of a block with the arrow at its end, which opens the list of that state
@Composable
private fun BlockHeader(title: String, onOpen: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = KaisouTheme.dimens.screenPadding), verticalAlignment = Alignment.CenterVertically) {
        BlockTitle(text = title, modifier = Modifier.weight(1f))
        SeeAllArrow(onClick = onOpen)
    }
}

// What the line under the name of a card of a list says: the day it was finished, how far they went, or its kind and year
private enum class EntryLine { Day, Progress, Facts }

// The titles of a state as a row of the cards of the Home that slides sideways: scores in the tab, the rating and how far they went
@Composable
private fun CardsRowBlock(title: String, entries: List<UserListEntry>, line: EntryLine, onOpen: () -> Unit, onOpenTitle: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens
    val locale = LocalConfiguration.current.locales[0]
    val dayFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val separator = stringResource(R.string.home_card_separator)

    // What finished shows the day it was, what is on the way shows how far it went, and what is planned its kind and year
    val cards = remember(entries, line, dayFormat) {
        entries.map { entry ->
            when (line) {
                EntryLine.Day -> entry.toCard(details = dayFormat.format(entry.finishedDate()))
                EntryLine.Progress -> entry.toCard(done = entry.progress, separator = separator)
                EntryLine.Facts -> entry.toCard(separator = separator)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        BlockHeader(title, onOpen)
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(cards, key = { it.anilistId }) { card ->
                TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide = false, onClick = { onOpenTitle(card.anilistId) })
            }
        }
    }
}

// A title of a list as the card of the Home has it: the scores of the person and of everyone, what rates it and its line
private fun UserListEntry.toCard(details: String = "", done: Int? = null, separator: String = ""): CardUi =
    CardUi(
        anilistId = mediaId,
        title = title,
        imageUrl = cover,
        score = average.takeIf { it > 0 }?.let { it / ScoreScale },
        episode = null,
        done = done,
        total = total,
        details = details.ifEmpty { listOfNotNull(format, year?.toString()).joinToString(separator) },
        query = null,
        rating = ratingQuery(),
        userScore = score.takeIf { it > 0 }?.let { it / ScoreScale }
    )

// The line under a name: the age rating, the kind and the year, the room of the rating being kept while it is looked for
@Composable
private fun EntryFacts(entry: UserListEntry) {
    val rating = rememberCardRating(remember(entry.mediaId) { entry.ratingQuery() })
    val showRating = !rating.loaded || rating.rating != null
    val facts = listOfNotNull(entry.format, entry.year?.toString()).joinToString(stringResource(R.string.home_card_separator))

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HomeDimens.CardDetailsGap)) {
        if (showRating) CardRating(rating.rating)
        if (facts.isNotEmpty()) Text(text = facts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// The scores at the end of a row: the one the person gave with the person icon and the one of everyone with the star
@Composable
private fun EntryScores(entry: UserListEntry) {
    val dimens = KaisouTheme.dimens
    val format = rememberScoreFormat()

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall), horizontalAlignment = Alignment.End) {
        if (entry.score > 0) ScoreLine(KaisouIcons.PersonFilled, format.format(entry.score / ScoreScale), stringResource(R.string.cd_your_score))
        if (entry.average > 0) ScoreLine(KaisouIcons.Star, format.format(entry.average / ScoreScale), null)
    }
}

// One score with its icon before it
@Composable
private fun ScoreLine(icon: ImageVector, text: String, description: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(HomeDimens.TabStar))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

// A title of a list as a cover, the card of the Home: its scores in the tab, and under its name the rating and how far they went
@Composable
internal fun ListPosterCard(entry: UserListEntry, onClick: () -> Unit) {
    val separator = stringResource(R.string.home_card_separator)
    val card = remember(entry, separator) { if (entry.progress > 0 && entry.status != "COMPLETED") entry.toCard(done = entry.progress, separator = separator) else entry.toCard(separator = separator) }

    TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide = false, onClick = onClick)
}

// A title of a list in a line: picture, name, kind and year, progress with a bar, and the score at the end
@Composable
internal fun ListLineCard(entry: UserListEntry, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val total = entry.total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = entry.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(UserDimens.RecentCover)
                .aspectRatio(HomeDimens.PosterRatio)
                .clip(MaterialTheme.shapes.small)
                .background(KaisouTheme.colors.skeleton)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            EntryFacts(entry)
            Text(text = progressText(entry), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
            if (total != null) {
                Box(modifier = Modifier.fillMaxWidth().height(TitleDimens.StatusBarHeight).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.background)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((entry.progress.toFloat() / total).coerceIn(0f, 1f)).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        EntryScores(entry)
    }
}

// How far they went, of the total when it is known
@Composable
private fun progressText(entry: UserListEntry): String =
    entry.total?.let { stringResource(R.string.user_list_progress, entry.progress, it) } ?: entry.progress.toString()

// The placeholder of the rows of the lists: two rows, each with the name of the block and cards that have a cover, a name and a line
@Composable
private fun ListRowsSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        repeat(ListRowsSkeletonBlocks) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = dimens.screenPadding)
                        .fillMaxWidth(UserDimens.SkeletonTitle)
                        .height(dimens.spaceLarge)
                        .skeleton()
                )
                Row(
                    modifier = Modifier.padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    repeat(ListRowsSkeletonCovers) {
                        Column(modifier = Modifier.width(HomeDimens.PosterWidth), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(HomeDimens.PosterRatio).skeleton(MaterialTheme.shapes.medium))
                            Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonName).height(ComponentDimens.SkeletonLineHeight).skeleton())
                            Box(modifier = Modifier.fillMaxWidth(UserDimens.SkeletonSince).height(ComponentDimens.SkeletonLineHeight).skeleton())
                        }
                    }
                }
            }
        }
    }
}

// The scores come from 0 to 100 and are shown from 0 to 10, and how many blocks and covers the placeholder has
private const val ScoreScale = 10f
private const val ListRowsSkeletonBlocks = 2
private const val ListRowsSkeletonCovers = 3
