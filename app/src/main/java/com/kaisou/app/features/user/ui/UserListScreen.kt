package com.kaisou.app.features.user.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.settings.ui.components.Choice
import com.kaisou.app.features.settings.ui.components.ChoiceSheet
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.CastRowsSkeleton
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.PickerPill
import com.kaisou.app.features.user.data.ListPreference
import com.kaisou.app.features.user.data.ListSort
import com.kaisou.app.features.user.data.ListTabs
import com.kaisou.app.features.user.data.UserListEntry
import com.kaisou.app.features.user.ui.components.ListLineCard
import com.kaisou.app.features.user.ui.components.KindSwitch
import com.kaisou.app.features.user.ui.components.ListPosterCard

// The Library, the whole list of the user as Dantotsu shows it: a tab per state, search, genre, order, random and grid or lines
@Composable
fun UserListScreen(onOpenTitle: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: UserListViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val grid by ListPreference.grid.collectAsState()
    val scope = rememberCoroutineScope()
    val sections = (state.sections as? Load.Done)?.value
    val tabs = remember(sections) { sections?.let { ListTabs.of(it) }.orEmpty() }
    var searching by rememberSaveable { mutableStateOf(state.search.isNotEmpty()) }
    val keys = stringArrayResource(R.array.title_list_keys)
    val names = stringArrayResource(if (state.manga) R.array.title_list_names_manga else R.array.title_list_names_anime)
    val genreKeys = stringArrayResource(R.array.genre_keys)
    val genreNames = stringArrayResource(R.array.genre_names)
    val allLabel = stringResource(R.string.user_list_all)
    val filterTitle = stringResource(R.string.user_list_filter)
    val sortTitle = stringResource(R.string.user_sort_sheet)

    // The name of a tab: the state in the language of the app, the name the person gave to a list of their own, or all
    val tabName: (String) -> String = { key -> if (key == ListTabs.All) allLabel else names.getOrNull(keys.indexOf(key)) ?: key }
    val genreName: (String) -> String = { genre -> genreNames.getOrNull(genreKeys.indexOf(genre)) ?: genre }

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
            Text(
                text = stringResource(R.string.nav_library),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceSmall + dimens.spaceExtraSmall)
            )
            HeaderAction(
                icon = KaisouIcons.Search,
                contentDescription = stringResource(R.string.cd_list_search),
                onClick = {
                    searching = !searching
                    if (!searching) viewModel.search("")
                },
                active = searching
            )
            HeaderAction(
                icon = KaisouIcons.Shuffle,
                contentDescription = stringResource(R.string.cd_list_random),
                onClick = { state.shown(state.tab).randomOrNull()?.let { onOpenTitle(it.mediaId) } }
            )
            HeaderAction(
                icon = if (grid) KaisouIcons.ListRows else KaisouIcons.LayoutGrid,
                contentDescription = stringResource(if (grid) R.string.cd_list_lines else R.string.cd_list_grid),
                onClick = { ListPreference.setGrid(!grid) }
            )
        }
        KindSwitch(manga = state.manga, onPick = viewModel::pickKind, modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceSmall))
        if (searching) SearchField(value = state.search, onChange = viewModel::search)

        when {
            state.sections is Load.Failed -> DetailsError(onRetry = viewModel::load)
            sections == null -> if (grid) CardsGridSkeleton(grouped = false) else CastRowsSkeleton(UserDimens.ListSkeletonRows)

            // The tabs start over for each kind, so each one has its own
            else -> key(state.manga) {
                val pager = rememberPagerState(initialPage = tabs.indexOfFirst { it.first == state.tab }.coerceAtLeast(0)) { tabs.size }

                // The tab follows the page that was swiped to
                LaunchedEffect(pager, tabs) {
                    snapshotFlow { pager.currentPage }.collect { page -> tabs.getOrNull(page)?.let { viewModel.pickTab(it.first) } }
                }

                // The tabs as Dantotsu has them: name and count, the open one in the accent with a line, sliding to keep it in view
                PrimaryScrollableTabRow(
                    selectedTabIndex = pager.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = dimens.screenPadding,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, (key, count) ->
                        Tab(
                            selected = pager.currentPage == index,
                            onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = { Text(text = stringResource(R.string.user_list_tab, tabName(key), count), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                        )
                    }
                }

                // The filter and the order, each opening the sheet of its choices
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val all = remember(sections) { ListTabs.all(sections) }
                    val genres = remember(all) { all.flatMap { it.genres }.distinct().sortedBy { genreName(it).lowercase() } }
                    PickerPill(
                        text = state.genre?.let(genreName) ?: filterTitle,
                        icon = KaisouIcons.Filter,
                        onClick = {
                            OverlaySheet.show {
                                ChoiceSheet(
                                    title = filterTitle,
                                    choices = listOf(Choice(allLabel, state.genre == null) { viewModel.pickGenre(null) }) +
                                        genres.map { genre -> Choice(genreName(genre), state.genre == genre) { viewModel.pickGenre(genre) } },
                                    onDismiss = OverlaySheet::dismiss
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    PickerPill(
                        text = stringResource(state.sort.label),
                        icon = KaisouIcons.ArrowUpDown,
                        onClick = {
                            OverlaySheet.show {
                                ChoiceSheet(
                                    title = sortTitle,
                                    choices = ListSort.entries.map { sort -> Choice(stringResource(sort.label), state.sort == sort) { viewModel.pickSort(sort) } },
                                    onDismiss = OverlaySheet::dismiss
                                )
                            }
                        }
                    )
                }
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { tabs[it].first }) { page ->
                    ListPage(entries = state.shown(tabs[page].first), grid = grid, onOpenTitle = onOpenTitle)
                }
            }
        }
    }
}

// The titles of a tab, in a grid of covers or in lines, or the words that say there is nothing
@Composable
private fun ListPage(entries: List<UserListEntry>, grid: Boolean, onOpenTitle: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens

    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.user_list_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceLarge)
        )
        return
    }
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        state = gridState,
        columns = if (grid) GridCells.Adaptive(HomeDimens.PosterWidth + dimens.spaceSmall) else GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(if (grid) dimens.spaceMedium else dimens.spaceSmall)
    ) {
        items(entries, key = { it.mediaId }) { entry ->
            if (grid) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    ListPosterCard(entry, onClick = { onOpenTitle(entry.mediaId) })
                }
            } else {
                ListLineCard(entry, onClick = { onOpenTitle(entry.mediaId) })
            }
        }
    }
}

// The field of the words to look for, which opens with the keyboard
@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val dimens = KaisouTheme.dimens
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding)
            .padding(top = dimens.spaceSmall)
            .clip(CircleShape)
            .background(KaisouTheme.colors.elevated)
            .padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = KaisouIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.ListIcon))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(text = stringResource(R.string.user_list_search), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
    }
}
