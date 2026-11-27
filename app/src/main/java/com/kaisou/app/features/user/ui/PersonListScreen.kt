package com.kaisou.app.features.user.ui

// Android back
import androidx.activity.compose.BackHandler
// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.components.CardsGridSkeleton
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.user.data.ListFilter
import com.kaisou.app.features.user.data.ListTabs
import com.kaisou.app.features.user.data.UserListEntry
import com.kaisou.app.features.user.ui.components.EmptyList
import com.kaisou.app.features.user.ui.components.ListPosterCard

// The list of anime or of manga of a person as AnymeX has it: a bar with the way back, name and actions, then the tabs and the cards
@Composable
fun PersonListScreen(onBack: () -> Unit, onOpenTitle: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: UserListViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val loaded = state.sections is Load.Done

    // The page is made again when the list comes, so the pages and the tab that was asked for start right
    key(loaded) { PersonListBody(viewModel, onBack, onOpenTitle, modifier) }
}

// The bar and the tabs on top and the pages under them, inside the room the screen leaves free around its cut and its edges
@Composable
private fun PersonListBody(viewModel: UserListViewModel, onBack: () -> Unit, onOpenTitle: (Int) -> Unit, modifier: Modifier) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val sections = (state.sections as? Load.Done)?.value
    val tabs = remember(sections) { sections?.let { ListTabs.pageTabs(it) }.orEmpty() }
    val pager = rememberPagerState(initialPage = tabs.indexOfFirst { it.first == state.tab }.coerceAtLeast(0)) { tabs.size }
    var searching by rememberSaveable { mutableStateOf(state.search.isNotEmpty()) }
    val keys = stringArrayResource(R.array.title_list_keys)
    val names = stringArrayResource(if (state.manga) R.array.title_list_names_manga else R.array.title_list_names_anime)
    val allLabel = stringResource(R.string.user_list_all)

    // The name of a tab: the state in the language of the app, the name the person gave to a list of their own, or all
    val tabName: (String) -> String = { key -> if (key == ListTabs.All) allLabel else names.getOrNull(keys.indexOf(key)) ?: key }

    // The back of the phone closes the search with the same fade before it leaves the page
    BackHandler(enabled = searching) {
        searching = false
        viewModel.search("")
    }

    // The tab follows the page that was swiped to
    LaunchedEffect(pager, tabs) {
        snapshotFlow { pager.currentPage }.collect { page -> tabs.getOrNull(page)?.let { viewModel.pickTab(it.first) } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        AnimatedContent(
            targetState = searching,
            transitionSpec = { fadeIn(tween(UserDimens.SearchMillis)) togetherWith fadeOut(tween(UserDimens.SearchMillis)) },
            label = "listBar"
        ) { searchOpen ->
            if (searchOpen) {
                SearchBar(
                    value = state.search,
                    onChange = viewModel::search,
                    onClose = {
                        searching = false
                        viewModel.search("")
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BarIcon(KaisouIcons.Back, stringResource(R.string.cd_back), MaterialTheme.colorScheme.primary, HomeDimens.HeaderIcon, onBack)
                    Text(
                        text = stringResource(if (state.manga) R.string.user_list_manga_of else R.string.user_list_anime_of, viewModel.owner),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = UserDimens.BarTitleSize, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceSmall)
                    )
                    BarIcon(KaisouIcons.Search, stringResource(R.string.cd_list_search), MaterialTheme.colorScheme.onSurfaceVariant) { searching = true }
                    BarIcon(KaisouIcons.Shuffle, stringResource(R.string.cd_list_random), MaterialTheme.colorScheme.onSurfaceVariant) {
                        state.shown(state.tab).randomOrNull()?.let { onOpenTitle(it.mediaId) }
                    }
                    BarIcon(
                        KaisouIcons.Filter,
                        stringResource(R.string.cd_list_filter),
                        if (state.filter.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        val everything = sections?.let { ListTabs.all(it) }.orEmpty()
                        OverlaySheet.show {
                            ListFilterSheet(
                                filter = state.filter,
                                entries = everything,
                                canFilterMine = state.mine != null,
                                manga = state.manga,
                                onApply = viewModel::pickFilter,
                                onDismiss = OverlaySheet::dismiss
                            )
                        }
                    }
                    BarIcon(KaisouIcons.ArrowUpDown, stringResource(R.string.cd_list_sort), MaterialTheme.colorScheme.onSurfaceVariant) {
                        OverlaySheet.show {
                            ListSortSheet(sort = state.sort, reversed = state.reversed, onApply = viewModel::pickSort, onDismiss = OverlaySheet::dismiss)
                        }
                    }
                }
            }
        }

        when {
            state.sections is Load.Failed -> DetailsError(onRetry = viewModel::load)
            sections == null -> CardsGridSkeleton(grouped = false)
            else -> {
                // The tabs with the name and the count as wide as their words, the open one in the accent over the elastic line
                PrimaryScrollableTabRow(
                    selectedTabIndex = pager.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = dimens.screenPadding,
                    indicator = { ElasticIndicator(pager, dimens.screenPadding) },
                    divider = {},
                    minTabWidth = 0.dp
                ) {
                    tabs.forEachIndexed { index, (key, count) ->
                        Tab(
                            selected = pager.currentPage == index,
                            onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = stringResource(R.string.user_list_tab, tabName(key), count).uppercase(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = UserDimens.TabTextSize, fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
                val onClear: (() -> Unit)? = if (state.filter.active || state.search.isNotBlank()) {
                    {
                        viewModel.pickFilter(ListFilter())
                        viewModel.search("")
                    }
                } else {
                    null
                }
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow),
                    key = { tabs[it].first }
                ) { page ->
                    // What the page shows is worked out again only when what shapes it changes, not on every draw of the screen
                    val entries = remember(state.sections, state.search, state.filter, state.mine, state.sort, state.reversed) { state.shown(tabs[page].first) }
                    TabGrid(entries = entries, onOpenTitle = onOpenTitle, onClear = onClear)
                }
            }
        }
    }
}

// The line under the words of the open tab, its start easing in and its end easing out between two tabs, as Dantotsu has it
@Composable
private fun TabIndicatorScope.ElasticIndicator(pager: PagerState, edge: Dp) {
    val color = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .tabIndicatorLayout { measurable, constraints, positions ->
                val spot = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, positions.lastIndex.toFloat())
                val left = floor(spot).toInt()
                val right = (left + 1).coerceAtMost(positions.lastIndex)
                val fraction = spot - left
                val from = positions[left]
                val to = positions[right]

                // The start eases in and the end eases out, so the line stretches toward the next tab and shrinks into it
                val startShare = 1f - cos(fraction * PI.toFloat() / 2f)
                val endShare = sin(fraction * PI.toFloat() / 2f)
                val fromStart = (from.left - edge + (from.width - from.contentWidth) / 2).toPx()
                val toStart = (to.left - edge + (to.width - to.contentWidth) / 2).toPx()
                val start = (fromStart + (toStart - fromStart) * startShare).roundToInt()
                val end = ((fromStart + from.contentWidth.toPx()) + ((toStart + to.contentWidth.toPx()) - (fromStart + from.contentWidth.toPx())) * endShare).roundToInt()

                val placeable = measurable.measure(Constraints.fixed(end - start, UserDimens.TabLine.roundToPx()))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(start, constraints.maxHeight - placeable.height)
                }
            }
            .background(color, RoundedCornerShape(topStart = UserDimens.TabLine, topEnd = UserDimens.TabLine))
    )
}

// An icon of the bar, round to touch
@Composable
private fun BarIcon(icon: ImageVector, description: String, tint: Color, glyph: Dp = UserDimens.BarGlyph, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(UserDimens.BarTouch).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(glyph))
    }
}

// The bar when the search is open: the way back that closes it, the field of the words and the button that clears them
@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarIcon(KaisouIcons.Back, stringResource(R.string.cd_back), MaterialTheme.colorScheme.primary, HomeDimens.HeaderIcon, onClose)
        Box(modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceSmall)) {
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
        if (value.isNotEmpty()) BarIcon(KaisouIcons.Close, stringResource(R.string.cd_list_clear), MaterialTheme.colorScheme.onSurfaceVariant) { onChange("") }
    }
}

// The titles of a tab as the cards of the Home in a grid that fits the width, or the face that says there is nothing
@Composable
private fun TabGrid(entries: List<UserListEntry>, onOpenTitle: (Int) -> Unit, onClear: (() -> Unit)?) {
    val dimens = KaisouTheme.dimens
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (entries.isEmpty()) {
        EmptyList(onClear = onClear)
        return
    }
    LazyVerticalGrid(
        state = rememberLazyGridState(),
        columns = GridCells.Adaptive(HomeDimens.PosterWidth + dimens.spaceSmall),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = dimens.screenPadding, end = dimens.screenPadding, top = dimens.spaceMedium, bottom = bottom + dimens.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        items(entries, key = { it.mediaId }) { entry ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                ListPosterCard(entry, onClick = { onOpenTitle(entry.mediaId) })
            }
        }
    }
}
