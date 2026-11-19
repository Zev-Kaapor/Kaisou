package com.kaisou.app.features.profile.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.kaisou.app.features.title.ui.components.DetailsSkeleton
import com.kaisou.app.features.title.ui.components.TitleTabs

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
                onSelect = { viewModel.chooseTab(tabs[it]) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortMenu(selected = state.sort, onSelect = viewModel::chooseSort)
            Spacer(modifier = Modifier.weight(1f))
            OnListBox(checked = state.onList, onChange = viewModel::chooseOnList)
        }
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
                            TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide = false, onClick = { onOpenTitle(card.anilistId) })
                        } else if (person != null) {
                            PersonCard(person, onOpen = { person.ref?.let(onOpenProfile) })
                        }
                    }
                }
            }
            when {
                state.failed -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) { DetailsError(onRetry = viewModel::loadMore) }
                state.loading || state.hasMore -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { DetailsSkeleton() }
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

// The sort in use with the arrows of sorting, which opens the menu of all of them, the one in use with a check
@Composable
private fun SortMenu(selected: MediaSort, onSelect: (MediaSort) -> Unit) {
    val dimens = KaisouTheme.dimens
    var open by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(vertical = dimens.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = KaisouIcons.ArrowUpDown, contentDescription = stringResource(R.string.profile_sort), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ListIcon))
            Text(text = stringResource(selected.label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Icon(imageVector = KaisouIcons.ChevronDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.ListIcon))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            MediaSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(sort.label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sort == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    },
                    trailingIcon = if (sort == selected) {
                        { Icon(imageVector = KaisouIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ListIcon)) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        onSelect(sort)
                    }
                )
            }
        }
    }
}

// A box to show only what is on the list of the user, with its words, a touch anywhere on it turns it
@Composable
private fun OnListBox(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Checkbox) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = KaisouTheme.colors.onAccentFill
            ),
            modifier = Modifier.padding(KaisouTheme.dimens.spaceSmall)
        )
        Text(text = stringResource(R.string.profile_on_list), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
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
