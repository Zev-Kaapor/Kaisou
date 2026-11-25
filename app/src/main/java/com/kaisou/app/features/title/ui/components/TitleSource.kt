package com.kaisou.app.features.title.ui.components

// Android resources
import androidx.annotation.StringRes
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.settings.ui.components.Choice
import com.kaisou.app.features.settings.ui.components.ChoiceSheet
import com.kaisou.app.features.title.data.EpisodeGroup
import com.kaisou.app.features.title.data.EpisodeSources
import com.kaisou.app.features.title.data.TitleEpisode
import com.kaisou.app.features.title.ui.TitleDimens

// The block of the source over the episodes, as Dantotsu has it: source, options, its title and the link to pick by hand
@Composable
internal fun SourceBlock(sourceId: String?, onSource: (String?) -> Unit) {
    val dimens = KaisouTheme.dimens
    val sources = EpisodeSources.installed
    val source = sources.firstOrNull { it.id == sourceId }
    val options = stringResource(R.string.title_options)

    // The room over it is the one under it, which is what the heading of the list has over it and what the link has under it
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceLarge + dimens.spaceSmall)) {
        Text(
            text = stringResource(R.string.title_source_title_none),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = dimens.spaceSmall)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TitleDimens.SourceFieldHeight)
                    .clip(MaterialTheme.shapes.medium)
                    .border(dimens.hairline, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .then(if (sources.isEmpty()) Modifier else Modifier.clickable(role = Role.DropdownList) { showSources(sourceId, onSource) })
                    .padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = KaisouIcons.ListVideo, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.SourceFieldIcon))
                if (sources.isEmpty()) {
                    // With no source installed the field only says so
                    Text(
                        text = stringResource(R.string.title_source_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.title_source), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = source?.name ?: stringResource(R.string.title_source_none),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(imageVector = KaisouIcons.ChevronDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.SortIcon))
                }
            }
            RoundAction(icon = KaisouIcons.Settings, description = options) { OverlaySheet.show { OptionsSheet(onDismiss = OverlaySheet::dismiss) } }
        }
        Text(
            text = stringResource(R.string.title_wrong_title),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button) { OverlaySheet.show { WrongTitleSheet(hasSource = source != null, onDismiss = OverlaySheet::dismiss) } }
                .padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceSmall)
        )
    }
}

// The sheet with the sources to choose from
internal fun showSources(sourceId: String?, onSource: (String?) -> Unit) {
    OverlaySheet.show {
        ChoiceSheet(
            title = stringResource(R.string.title_source),
            choices = listOf(Choice(stringResource(R.string.title_source_none), sourceId == null) { onSource(null) }) +
                EpisodeSources.installed.map { source -> Choice(source.name, source.id == sourceId) { onSource(source.id) } },
            onDismiss = OverlaySheet::dismiss
        )
    }
}

// The name of the list, with how far the user is under it, and on the end the pill of the order of the list, which opens its choices
@Composable
internal fun EpisodesHeading(watched: Int, total: Int, newestFirst: Boolean, onSort: (Boolean) -> Unit) {
    val dimens = KaisouTheme.dimens
    val newest = stringResource(R.string.title_episodes_sort_new)
    val oldest = stringResource(R.string.title_episodes_sort_old)
    val sortTitle = stringResource(R.string.profile_sort)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceLarge),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.title_tab_episodes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = if (watched > 0) pluralStringResource(R.plurals.title_episodes_progress, watched, watched, total) else pluralStringResource(R.plurals.home_card_episodes, total, total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        PickerPill(
            text = if (newestFirst) newest else oldest,
            icon = if (newestFirst) KaisouIcons.SortDescending else KaisouIcons.SortAscending,
            fill = KaisouTheme.colors.elevated.copy(alpha = TitleDimens.PillFill),
            onClick = {
                OverlaySheet.show {
                    ChoiceSheet(
                        title = sortTitle,
                        choices = listOf(
                            Choice(oldest, !newestFirst, icon = KaisouIcons.SortAscending) { onSort(false) },
                            Choice(newest, newestFirst, icon = KaisouIcons.SortDescending) { onSort(true) }
                        ),
                        onDismiss = OverlaySheet::dismiss
                    )
                }
            }
        )
    }
}

// The card to go on, thin over the picture of the next episode: its darkened picture, its line, its name, the play and a progress bar
@Composable
internal fun ContinueCard(episode: TitleEpisode, progress: Float, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val page = MaterialTheme.colorScheme.background
    val name = episode.title?.let { stringResource(R.string.title_continue_named, episode.number, it) } ?: stringResource(R.string.title_episode_untitled, episode.number)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding)
            .padding(top = dimens.spaceMedium)
            .height(TitleDimens.ContinueHeight)
            .clip(MaterialTheme.shapes.large)
            .background(KaisouTheme.colors.skeleton)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        episode.image?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(listOf(page.copy(alpha = TitleDimens.ContinueScrim), page.copy(alpha = TitleDimens.ContinueScrimEnd))))
        )
        Row(
            modifier = Modifier.matchParentSize().padding(horizontal = dimens.spaceMedium),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                Text(text = stringResource(R.string.title_continue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.size(TitleDimens.ContinuePlay).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = KaisouIcons.PlayOutline, contentDescription = null, tint = KaisouTheme.colors.onAccentFill, modifier = Modifier.size(TitleDimens.ContinuePlayIcon))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(TitleDimens.ContinueProgress)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = TitleDimens.ContinueTrack))
        ) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(TitleDimens.ContinueProgress).background(MaterialTheme.colorScheme.primary))
        }
    }
}

// The seasons, or the ranges of a long series, as chips in a row that scrolls, the one that is open filled in the accent
@Composable
internal fun GroupChips(groups: List<EpisodeGroup>, selected: Int, onSelect: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens
    val scroll = rememberScrollState()

    // Where each chip is in the row and how wide, and how wide the row shows, to bring the open one to the middle
    val places = remember { mutableStateMapOf<Int, IntRange>() }
    var width by remember { mutableIntStateOf(0) }

    LaunchedEffect(selected, width, places.size) {
        val place = places[selected] ?: return@LaunchedEffect
        scroll.animateScrollTo(((place.first + place.last) / 2 - width / 2).coerceAtLeast(0))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceMedium)
            .onSizeChanged { width = it.width }
            .horizontalScroll(scroll)
            .padding(horizontal = dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        groups.forEachIndexed { index, group ->
            val isOpen = index == selected
            Text(
                text = if (group.season != null) stringResource(R.string.title_episodes_season, group.season) else stringResource(R.string.title_episodes_range, group.first, group.last),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isOpen) FontWeight.Bold else FontWeight.Medium),
                color = if (isOpen) KaisouTheme.colors.onAccentFill else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .onGloballyPositioned { places[index] = it.positionInParent().x.roundToInt()..(it.positionInParent().x.roundToInt() + it.size.width) }
                    .clip(CircleShape)
                    .background(if (isOpen) MaterialTheme.colorScheme.primary else KaisouTheme.colors.elevated)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceSmall)
            )
        }
    }
}

// A round button, with the color of the pills
@Composable
private fun RoundAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TitleDimens.SourceAction)
            .clip(CircleShape)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(TitleDimens.SourceActionIcon))
    }
}

// A line of the lists of the sheets of the source: its icon, its name and what it does
private class OptionLine(val icon: ImageVector, @StringRes val name: Int, @StringRes val note: Int)

// The lines of a card for the options and the title, each with an icon, name, description and arrow, coming with extensions
@Composable
private fun OptionList(lines: List<OptionLine>) {
    val dimens = KaisouTheme.dimens
    val soon = stringResource(R.string.title_needs_extensions)

    Column(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dimens.spaceMedium),
                    thickness = dimens.hairline,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { Toasts.show(soon) }
                    .padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = line.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.OptionIcon))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    Text(text = stringResource(line.name), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                    Text(text = stringResource(line.note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.SortIcon))
            }
        }
    }
}

// The sheet to pick by hand the title the source has for this anime, which needs a source first
@Composable
private fun WrongTitleSheet(hasSource: Boolean, onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens

    KaisouSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = dimens.screenPadding).padding(bottom = dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            SheetTitle(stringResource(R.string.title_wrong_title))
            Text(text = stringResource(R.string.title_wrong_title_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OptionList(
                listOf(
                    OptionLine(
                        KaisouIcons.Search,
                        R.string.title_wrong_title_search,
                        if (hasSource) R.string.title_wrong_title_search_note else R.string.title_wrong_title_search_need
                    )
                )
            )
        }
    }
}

// The options of the source as Dantotsu has them: the extension settings, Cloudflare, torrent, the site, many downloads and the reset
@Composable
private fun OptionsSheet(onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens

    KaisouSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = dimens.screenPadding).padding(bottom = dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            SheetTitle(stringResource(R.string.title_options))
            OptionList(
                listOf(
                    OptionLine(KaisouIcons.Settings, R.string.title_option_source, R.string.title_option_source_note),
                    OptionLine(KaisouIcons.Shield, R.string.title_option_cloudflare, R.string.title_option_cloudflare_note),
                    OptionLine(KaisouIcons.Magnet, R.string.title_option_torrent, R.string.title_option_torrent_note),
                    OptionLine(KaisouIcons.ExternalLink, R.string.title_option_website, R.string.title_option_website_note),
                    OptionLine(KaisouIcons.Download, R.string.title_option_download, R.string.title_option_download_note),
                    OptionLine(KaisouIcons.Trash, R.string.title_option_reset, R.string.title_option_reset_note)
                )
            )
        }
    }
}

// The name at the top of a sheet
@Composable
private fun SheetTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
}
