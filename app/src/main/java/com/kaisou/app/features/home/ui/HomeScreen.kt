package com.kaisou.app.features.home.ui

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kaisou.app.core.designsystem.components.rememberKeptListState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
// View model
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniViewer
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.HiddenOnHome
import com.kaisou.app.features.home.data.HomeLayout
import com.kaisou.app.features.home.data.HomeStatus
import com.kaisou.app.features.home.data.StoryUi
import com.kaisou.app.features.home.ui.components.HomeHeader
import com.kaisou.app.features.settings.data.AdultContent
import com.kaisou.app.features.home.ui.components.HomeHeroCarousel
import com.kaisou.app.features.home.ui.components.HomeFriends
import com.kaisou.app.features.home.ui.components.HomeHeroSkeleton
import com.kaisou.app.features.home.ui.components.HomeShelf
import com.kaisou.app.features.home.ui.components.homeShelvesSkeleton
import com.kaisou.app.features.home.ui.components.StatusEditor
import com.kaisou.app.features.home.ui.components.StoryViewer
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.features.title.ui.components.ListEditorSheet

// Home of the user: the hero at the top with the header over it, the rest of the screen arrives piece by piece
@Composable
fun HomeScreen(
    viewer: AniViewer,
    onProfile: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenList: (manga: Boolean, tab: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberKeptListState(viewModel.scroll)

    // The news of the friends come without pulling, when the user comes back and every few minutes while the Home is shown
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.setShown(true)
            try {
                viewModel.refreshIfStale(HomeDimens.ResumeRefreshMinMillis)
                while (true) {
                    delay(HomeDimens.AutoRefreshMillis)

                    // A little less than the time between, since the Home was asked for a moment after the last time it was due
                    viewModel.refreshIfStale(HomeDimens.AutoRefreshMillis / 2)
                }
            } finally {
                viewModel.setShown(false)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight

        // Standing up the hero is as tall as the screen is wide times a ratio, lying down it takes a share of the height
        val heroHeight = if (landscape) maxHeight * HomeDimens.HeroLandscapeHeightFraction else maxWidth * HomeDimens.HeroAspect

        // The header is clear over the hero and turns solid before the details get under it, so its icons never sit over the text
        val solidOffset = with(LocalDensity.current) { heroHeight.toPx() } * HomeDimens.HeaderSolidFraction

        // How far along the fade is: none at the top, all where the details get under the header, following the scroll in between
        val headerProgress by remember(solidOffset) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / solidOffset).coerceIn(0f, 1f)
            }
        }

        // The user and the friends that have something to show, in the order the stories play, and the person whose story is open
        val own = remember(viewer, state.ownStory) { StoryUi(viewer.id, viewer.name, viewer.avatar, state.ownStory) }
        val playable = remember(own, state.friends) { (listOf(own) + state.friends).filter { it.items.isNotEmpty() } }
        var openStory by viewModel::openStory


        // The shelves without what the user hid or turned off or the adult choice leaves out, and without the ones left with none
        val hiddenOnHome by HiddenOnHome.ids.collectAsState()
        val adultMode by AdultContent.mode.collectAsState()
        val layout by HomeLayout.state.collectAsState()
        val shelves = remember(state.shelves, hiddenOnHome, adultMode, layout) {
            state.shelves.filter { layout.shown(it.id.name) }.map { shelf -> shelf.copy(cards = shelf.cards.filter { it.anilistId !in hiddenOnHome && AdultContent.allows(it.rating.adult) }) }.filter { it.cards.isNotEmpty() }
        }

        when (state.status) {
            // The placeholders of the hero and of what is under it, with no pulling to refresh while they are there
            HomeStatus.Loading -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(HomeDimens.SectionSpacing)
            ) {
                item(key = "hero-skeleton") { HomeHeroSkeleton(height = heroHeight, landscape = landscape) }
                homeShelvesSkeleton()
            }
            HomeStatus.Error -> HomeError(onRetry = viewModel::load)
            HomeStatus.Content -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
              LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(HomeDimens.SectionSpacing)
              ) {
                item(key = "hero") {
                    HomeHeroCarousel(
                        heroes = state.heroes,
                        index = state.heroIndex,
                        onIndexChange = viewModel::showHero,
                        art = state.art,
                        posterSize = state.posterSize,
                        backdropSize = state.backdropSize,
                        logoSize = state.logoSize,
                        height = heroHeight,
                        landscape = landscape,
                        onEditList = { id ->
                            scope.launch {
                                val editor = viewModel.listEditor(id) ?: return@launch
                                OverlaySheet.show {
                                    ListEditorSheet(
                                        title = editor.title,
                                        options = editor.options,
                                        onSave = { draft -> viewModel.saveEntry(id, draft) },
                                        onRemove = { viewModel.removeEntry(editor.title.entry?.id) },
                                        onDismiss = OverlaySheet::dismiss
                                    )
                                }
                            }
                        },
                        onOpenTitle = onOpenTitle
                    )
                }

                // What is under the hero: the user and the friends, then the shelves that have something on them
                if (layout.shown(HomeLayout.Stories)) item(key = "friends") { HomeFriends(own = own, friends = state.friends, seen = state.seen, onOpen = { openStory = it.userId }, onAdd = viewModel::openComposer) }
                items(shelves, key = { it.id.name }) { shelf -> HomeShelf(shelf = shelf, onOpenTitle = onOpenTitle, onOpenList = onOpenList) }
              }
            }
        }

        HomeHeader(
            viewer = viewer,
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = headerProgress),
            onNotifications = {},
            onSearch = {},
            onProfile = onProfile,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // The screen to write a status
        state.composer?.let { composer ->
            StatusEditor(composer = composer, onPost = viewModel::postStatus, onClose = viewModel::closeComposer)
        }

        openStory?.let { userId ->
            val start = playable.indexOfFirst { it.userId == userId }
            if (start >= 0) {
                StoryViewer(
                    stories = playable,
                    startUser = start,
                    startItem = viewModel.storyItem,
                    onPosition = { userId, itemId ->
                        openStory = userId
                        viewModel.storyItem = itemId
                    },
                    ownUserId = viewer.id,
                    seen = state.seen,
                    replies = state.replies,
                    onDelete = viewModel::deleteActivity,
                    onSeen = viewModel::markSeen,
                    onLike = viewModel::toggleLike,
                    onOpenReplies = viewModel::openReplies,
                    onLikeReply = viewModel::toggleReplyLike,
                    onDeleteReply = viewModel::deleteReply,
                    onEditReply = viewModel::startEditingReply,
                    onCancelEdit = viewModel::cancelEditingReply,
                    onSendReply = viewModel::sendReply,
                    onCloseReplies = viewModel::closeReplies,
                    onOpenTitle = { id ->
                        viewModel.closeReplies()
                        openStory = null
                        viewModel.storyItem = null
                        onOpenTitle(id)
                    },
                    onClose = {
                        viewModel.closeReplies()
                        openStory = null
                        viewModel.storyItem = null
                    }
                )
            }
        }
    }
}

// Shown when AniList could not be reached and there is nothing to show, a tap asks again
@Composable
private fun HomeError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onRetry)
            .padding(dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.home_retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
