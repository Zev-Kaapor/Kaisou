package com.kaisou.app.features.profile.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
// Material 3 components
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Pictures
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.home.ui.components.TitleCard
import com.kaisou.app.features.profile.data.MediaSort
import com.kaisou.app.features.profile.data.MediaTab
import com.kaisou.app.features.profile.data.ProfileKind
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.TitlePerson
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.TitleTabs
import androidx.compose.foundation.shape.CircleShape
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.features.settings.ui.components.Choice
import com.kaisou.app.features.settings.ui.components.ChoiceSheet
import com.kaisou.app.features.title.ui.components.PickerPill
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.PostersRowSkeleton
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp

// All the titles of a character, a person of the staff or a studio, with back and the name of what they are on top
@Composable
fun ProfileMediaScreen(
    onBack: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileMediaViewModel = viewModel()
) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAction(icon = KaisouIcons.Back, contentDescription = stringResource(R.string.cd_back), onClick = onBack)
            Text(
                text = stringResource(
                    when (viewModel.ref.kind) {
                        ProfileKind.Character -> R.string.profile_appears
                        ProfileKind.Staff -> R.string.profile_works
                        ProfileKind.Studio -> R.string.profile_productions
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        ProfileMediaContent(viewModel, onOpenTitle, onOpenProfile)
    }
}

// The tabs of a person, as AniList has them: what a person voiced, the anime and the manga. Under them two things to choose: how the titles
// are sorted, in a menu, and a box to show only what is on the list of the user. Then the titles in a grid that asks for more as it gets
// to its end. Sorted by the newest or the oldest they are grouped by year, with how many there are in each year
@Composable
fun ProfileMediaContent(viewModel: ProfileMediaViewModel, onOpenTitle: (Int) -> Unit, onOpenProfile: (ProfileRef) -> Unit) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState(viewModel.scroll.index, viewModel.scroll.offset)
    // A character is in anime and in manga, a person of the staff also voices characters, a studio only makes anime
    val tabs = when (viewModel.ref.kind) {
        ProfileKind.Character -> listOf(MediaTab.Anime, MediaTab.Manga)
        ProfileKind.Staff -> listOf(MediaTab.Voices, MediaTab.Anime, MediaTab.Manga)
        ProfileKind.Studio -> emptyList()
    }

    // The position is kept by the view model, so it is the same when the phone is turned
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            viewModel.scroll.index = index
            viewModel.scroll.offset = offset
        }
    }
    // A filter that changes starts the list again, and the grid goes back to its top
    LaunchedEffect(state.restarts) {
        if (state.restarts > 0) gridState.scrollToItem(0)
    }
    // The next page is asked for when the last rows are on the screen
    LaunchedEffect(gridState, state.items.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= gridState.layoutInfo.totalItemsCount - LoadAhead) viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (tabs.isNotEmpty()) {
            TitleTabs(
                labels = tabs.map { stringResource(it.label) },
                selected = tabs.indexOf(state.tab).coerceAtLeast(0),
                onSelect = { viewModel.chooseTab(tabs[it]) },
                top = 0.dp
            )
        }
        // How the titles are sorted, in a sheet, and the capsule that shows only what is on the list of the user
        val sortTitle = stringResource(R.string.profile_sort)
        val sortLabels = MediaSort.entries.associateWith { stringResource(it.label) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerPill(
                text = stringResource(state.sort.label),
                icon = KaisouIcons.ArrowUpDown,
                onClick = {
                    OverlaySheet.show {
                        ChoiceSheet(
                            title = sortTitle,
                            choices = MediaSort.entries.map { sort -> Choice(sortLabels.getValue(sort), sort == state.sort) { viewModel.chooseSort(sort) } },
                            onDismiss = OverlaySheet::dismiss
                        )
                    }
                }
            )
            OnListPill(checked = state.onList, onChange = viewModel::chooseOnList)
        }
        if (state.items.isEmpty() && (state.loading || state.hasMore) && !state.failed) {
            CardsGridSkeleton(grouped = state.sort.byYear)
            return@Column
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(HomeDimens.PosterWidth + dimens.spaceSmall),
                modifier = Modifier.fillMaxSize().scrollIndicator(gridState),
                contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                val groups = if (state.sort.byYear) state.items.groupBy { it.year } else mapOf(null to state.items)
                groups.forEach { (year, inYear) ->
                    if (state.sort.byYear) {
                        item(key = "year-$year", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(
                                    R.string.profile_year_count,
                                    year?.toString() ?: stringResource(R.string.profile_year_unknown),
                                    pluralStringResource(R.plurals.profile_titles_count, inYear.size, inYear.size)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    items(inYear, key = { it.key }) { item ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            val card = item.card
                            val person = item.person
                            if (card != null) {
                                TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide = false, onClick = { onOpenTitle(card.anilistId) }, detailsLines = 2)
                            } else if (person != null) {
                                PersonCard(person, onOpen = { person.ref?.let(onOpenProfile) })
                            }
                        }
                    }
                }
                when {
                    state.failed -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) { DetailsError(onRetry = viewModel::loadMore) }
                    state.loading || state.hasMore -> item(key = if (state.items.isEmpty()) "loading-first" else "loading-more", span = { GridItemSpan(maxLineSpan) }) { PostersRowSkeleton() }
                    state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.profile_media_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(dimens.screenPadding)
                        )
                    }
                }
            }
        }
    }
}

// The capsule that shows only what is on the list of the user. It has the icon of the list even when it is off, so it reads as something to
// touch, with its fill like the others; on, it takes the color of the app and the words in the color over it. A touch anywhere on it turns it
@Composable
private fun OnListPill(checked: Boolean, onChange: (Boolean) -> Unit) {
    val dimens = KaisouTheme.dimens
    val content = if (checked) KaisouTheme.colors.onAccentFill else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else KaisouTheme.colors.elevated)
            .clickable(role = Role.Checkbox) { onChange(!checked) }
            .padding(start = dimens.spaceMedium, end = dimens.spaceMedium, top = dimens.spaceExtraSmall, bottom = dimens.spaceExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) KaisouIcons.Check else KaisouIcons.ListChecks,
            contentDescription = null,
            tint = if (checked) content else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(TitleDimens.SortIcon)
        )
        Text(text = stringResource(R.string.profile_on_list), style = MaterialTheme.typography.bodySmall, color = content)
    }
}

// A character a person voiced: its picture, its name, and the title it is in with its part, a touch opens the character
@Composable
private fun PersonCard(person: TitlePerson, onOpen: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.width(HomeDimens.PosterWidth).clickable(role = Role.Button, onClick = onOpen)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HomeDimens.PosterRatio)
                .clip(MaterialTheme.shapes.medium)
                .background(KaisouTheme.colors.skeleton)
        ) {
            person.image?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Text(
            text = person.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceExtraSmall)
        )
        listOfNotNull(person.role, person.voice).forEach { line ->
            Text(text = line, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// How many items before the end of the grid the next page is asked for
private const val LoadAhead = 6
