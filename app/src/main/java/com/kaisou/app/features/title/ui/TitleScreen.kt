package com.kaisou.app.features.title.ui

// Android framework
import android.content.Intent
// Compose animation
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// View model
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.alerts.ReleaseAlerts
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.home.ui.components.HomeHeroSkeleton
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.ReleaseState
import com.kaisou.app.features.title.data.TitleStatus
import com.kaisou.app.features.title.ui.components.ListEditorSheet
import com.kaisou.app.features.title.ui.components.NextEpisodeCard
import com.kaisou.app.features.title.ui.components.TitleFacts
import com.kaisou.app.features.title.ui.components.TitleHero
import com.kaisou.app.features.title.ui.components.TitleSynopsis
import com.kaisou.app.features.title.ui.components.TitleTab
import com.kaisou.app.features.title.ui.components.TitleTabs
import com.kaisou.app.features.title.ui.components.titleCommunityItems
import com.kaisou.app.features.title.ui.components.titleDetailsItems
import com.kaisou.app.features.title.ui.components.titleEpisodesItems

// The page of a title, anime or manga. Prototype, to be thrown away when the look is decided: for now it has the top and the synopsis,
// the rest comes part by part
@Composable
fun TitleScreen(
    onBack: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenNews: (id: Int, malId: Int?, manga: Boolean) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    onOpenCast: (id: Int, staff: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TitleViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)
    var tab by viewModel::tab
    var newestFirst by viewModel::newestFirst
    // The tabs that ask for their own data do it when they are opened
    LaunchedEffect(tab, state.status) { if (state.status == TitleStatus.Content) viewModel.openTab(tab) }
    val alerts by ReleaseAlerts.titles.collectAsState()
    // The tabs of a manga have chapters where an anime has episodes
    val tabLabels = listOf(
        stringResource(R.string.title_tab_details),
        stringResource(if (state.title?.manga == true) R.string.title_tab_chapters else R.string.title_tab_episodes),
        stringResource(R.string.title_tab_community)
    )

    // Each time the page comes back to the screen what AniList says is asked again, so the heart and the list are never behind
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val landscape = maxWidth > maxHeight
        // Standing up the art is a poster as tall as the screen is wide times a ratio, lying down it takes a share of the height, so
        // the synopsis shows under it
        val heroHeight = if (landscape) maxHeight * HomeDimens.HeroLandscapeHeightFraction else maxWidth * HomeDimens.HeroAspect

        // The bar is clear over the art and turns solid, with the name of the title, before the details get under it
        val solidOffset = with(LocalDensity.current) { heroHeight.toPx() } * HomeDimens.HeaderSolidFraction
        // How far along the fade is: none at the top, all of it where the details get under the bar, in between it follows the scroll
        val barProgress by remember(solidOffset) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / solidOffset).coerceIn(0f, 1f)
            }
        }

        when (state.status) {
            TitleStatus.Loading -> LazyColumn(modifier = Modifier.fillMaxSize(), userScrollEnabled = false) {
                item(key = "hero-skeleton") { HomeHeroSkeleton(height = heroHeight, landscape = landscape) }
            }
            TitleStatus.Error -> TitleError(onRetry = viewModel::load)
            TitleStatus.Content -> state.title?.let { title ->
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item(key = "hero") {
                        TitleHero(
                            title = title,
                            art = state.art,
                            posterSize = state.posterSize,
                            backdropSize = state.backdropSize,
                            logoSize = state.logoSize,
                            landscape = landscape,
                            // The list button has no action yet, the heart is the favourite of AniList
                            onListButton = {},
                            onEditList = {
                                OverlaySheet.show {
                                    ListEditorSheet(
                                        title = title,
                                        options = state.listOptions,
                                        onSave = viewModel::saveEntry,
                                        onRemove = viewModel::removeEntry,
                                        onDismiss = OverlaySheet::dismiss
                                    )
                                }
                            },
                            onFavourite = viewModel::toggleFavourite,
                            modifier = Modifier.fillMaxWidth().height(heroHeight)
                        )
                    }
                    val synopsis = state.art?.synopsis ?: title.synopsis
                    if (synopsis.isNotBlank()) item(key = "synopsis") { TitleSynopsis(text = synopsis) }
                    item(key = "facts") { TitleFacts(title = title) }
                    title.nextAiring?.let { next -> item(key = "next-episode") { NextEpisodeCard(next = next) } }
                    item(key = "tabs") { TitleTabs(labels = tabLabels, selected = tab.ordinal, onSelect = { tab = TitleTab.entries[it] }) }
                    // Each tab shows its own blocks
                    when (tab) {
                        TitleTab.Details -> titleDetailsItems(
                            details = state.details,
                            watchOrder = state.watchOrder,
                            people = state.people,
                            recommendations = state.recommendations,
                            themes = state.themes,
                            news = state.news,
                            covers = state.covers,
                            onOpenTitle = onOpenTitle,
                            onRetryExtras = viewModel::loadExtras,
                            onRetryNews = viewModel::retryNews,
                            onOpenNews = { onOpenNews(title.anilistId, title.malId, title.manga) },
                            onOpenProfile = onOpenProfile,
                            onOpenCast = { staff -> onOpenCast(title.anilistId, staff) },
                            onRetryWatchOrder = viewModel::retryWatchOrder
                        )
                        TitleTab.Episodes -> titleEpisodesItems(
                            title = title,
                            episodes = state.episodes,
                            newestFirst = newestFirst,
                            onToggleOrder = { newestFirst = !newestFirst },
                            onRetry = viewModel::retryEpisodes
                        )
                        TitleTab.Community -> titleCommunityItems(state.community, onRetry = viewModel::retryCommunity)
                    }
                    item(key = "end") { Spacer(modifier = Modifier.height(KaisouTheme.dimens.spaceLarge)) }
                }
            }
        }

        val context = LocalContext.current
        val siteUrl = state.title?.siteUrl
        val alertsOn = state.title?.anilistId in alerts
        val alertOnMessage = stringResource(R.string.title_alert_on)
        val alertOffMessage = stringResource(R.string.title_alert_off)
        val alertOffNote = stringResource(R.string.title_alert_off_note)
        // What the bell is for depends on where the title is: each new episode or chapter, a new season or what comes after, the return
        // after a pause, or the release. A title that was cancelled has nothing to follow. The notices are not there yet, so the island
        // says what they will do
        val manga = state.title?.manga == true
        val notifyText = when (state.title?.releaseState) {
            ReleaseState.Releasing -> if (manga) R.string.title_notify_releasing_manga else R.string.title_notify_releasing
            ReleaseState.Finished -> if (manga) R.string.title_notify_finished_manga else R.string.title_notify_finished
            ReleaseState.Hiatus -> R.string.title_notify_hiatus
            ReleaseState.NotYetReleased -> R.string.title_notify_upcoming
            else -> null
        }?.let { stringResource(it) }
        TitleTopBar(
            name = state.title?.title,
            progress = barProgress,
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = barProgress),
            onBack = onBack,
            alertsOn = alertsOn,
            onNotify = notifyText?.let { text ->
                {
                    state.title?.let { title ->
                        val on = ReleaseAlerts.toggle(title.anilistId)
                        Toasts.show(
                            message = if (on) alertOnMessage else alertOffMessage,
                            subtitle = if (on) text else alertOffNote,
                            icon = if (on) KaisouIcons.BellRinging else KaisouIcons.Bell
                        )
                    }
                }
            },
            onShare = siteUrl?.let { url ->
                {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// Back on the left and share on the right over the art, and the name of the title between them once the bar is solid. The room of the
// status bar stays even when it is hidden, so the bar keeps clear of the camera
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleTopBar(
    name: String?,
    progress: Float,
    containerColor: Color,
    onBack: () -> Unit,
    alertsOn: Boolean,
    onNotify: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderAction(
            icon = KaisouIcons.Back,
            contentDescription = stringResource(R.string.cd_back),
            onClick = onBack
        )
        Text(
            text = name.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimens.spaceSmall)
                .alpha(progress)
        )
        // The bell rings when the alerts of the title are on
        if (onNotify != null) {
            HeaderAction(
                icon = if (alertsOn) KaisouIcons.BellRinging else KaisouIcons.Bell,
                contentDescription = stringResource(R.string.cd_notify),
                onClick = onNotify
            )
        }
        if (onShare != null) {
            HeaderAction(
                icon = KaisouIcons.Share,
                contentDescription = stringResource(R.string.cd_share),
                onClick = onShare
            )
        }
    }
}

// Shown when AniList could not be reached or does not know the title, a tap asks again
@Composable
private fun TitleError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.title_error),
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
