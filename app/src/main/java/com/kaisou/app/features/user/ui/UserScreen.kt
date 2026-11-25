package com.kaisou.app.features.user.ui

// Android framework
import android.content.Intent
import java.text.NumberFormat
import java.util.Locale
// Compose layouts and gestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
// View model and images
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.navigation.NavSections
import com.kaisou.app.core.navigation.NavSectionsHost
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.EnterScope
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.components.Chip
import com.kaisou.app.features.title.ui.components.TagTip
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.user.ui.components.UserSkeleton
import com.kaisou.app.features.user.data.FavouriteKind
import com.kaisou.app.features.user.data.UserUi
import com.kaisou.app.features.user.ui.components.userOverviewItems

// The page of a person of AniList: the banner under back and share that turn into a solid bar, picture, name, follow and sections
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserScreen(
    onBack: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    onOpenActivity: (name: String, id: Int) -> Unit,
    onOpenFavourites: (name: String, kind: FavouriteKind) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = viewModel()
) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val counts by viewModel.counts.collectAsState()
    val translate by viewModel.translate.collectAsState()
    val translatedAbout by viewModel.about.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)
    val context = LocalContext.current
    val user = (state as? Load.Done)?.value
    val density = LocalDensity.current
    var tab by viewModel::tab
    val scope = rememberCoroutineScope()

    // How far the items of the section that was just opened have come in, from nothing to all, and how far to the side they start
    var enter by remember { mutableFloatStateOf(1f) }
    var enterShift by remember { mutableFloatStateOf(0f) }

    // The sections go to the bar of the app while the page is there, and are taken from it when the page goes
    val barOwner = remember { Any() }
    DisposableEffect(barOwner) { onDispose { NavSectionsHost.clear(barOwner) } }
    val tabLabels = UserTab.entries.map { stringResource(it.label) }
    val statusBar = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()

    // The bar is clear over the banner and turns solid before the page gets under it, following the scroll
    val solidOffset = with(density) { (UserDimens.BannerHeight + statusBar).toPx() } * HomeDimens.HeaderSolidFraction
    val barProgress by remember(solidOffset) {
        derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / solidOffset).coerceIn(0f, 1f) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        val shiftPx = with(density) { maxWidth.toPx() } * TitleDimens.SectionShift

        // Opens a section: its items slide in from the side it is on, the page stays where it is
        val openTab: (Int) -> Unit = { index ->
            if (index != tab.ordinal) {
                enterShift = if (index > tab.ordinal) shiftPx else -shiftPx
                enter = 0f
                tab = UserTab.entries[index]
                scope.launch { animate(0f, 1f, animationSpec = tween(KaisouMotion.ExpandMillis)) { value, _ -> enter = value } }
            }
        }

        // The section that is open is read here, in the composition, so the bar follows it when it changes
        val openIndex = tab.ordinal
        if (user != null) SideEffect { NavSectionsHost.show(barOwner, NavSections(tabLabels, UserTab.entries.map { it.icon }, openIndex), openTab) }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().scrollIndicator(listState)) {
            when {
                user != null -> {
                    item(key = "header") { UserHeader(user, counts, bannerTop = statusBar, onFollow = viewModel::toggleFollow) }

                    // Each section shows its own blocks, which come in as it is opened
                    with(EnterScope(this, { enter }, { enterShift })) {
                        when (tab) {
                            UserTab.Overview -> userOverviewItems(
                                if (translate) user.copy(about = translatedAbout ?: user.about) else user,
                                recent,
                                onOpenTitle,
                                onOpenProfile,
                                onOpenActivity = { onOpenActivity(user.name, user.id) },
                                onOpenFavourites = { onOpenFavourites(user.name, it) }
                            )
                            else -> item(key = "soon-${tab.name}") { SoonBlock() }
                        }
                    }
                    item(key = "end") { Spacer(modifier = Modifier.height(dimens.spaceLarge)) }
                }
                state is Load.Failed -> item(key = "error") { Column(modifier = Modifier.padding(top = statusBar + dimens.spaceLarge)) { DetailsError(onRetry = viewModel::load) } }
                else -> item(key = "loading") { UserSkeleton(bannerTop = statusBar, showFollow = !viewModel.isSelf) }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = barProgress))
                .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                .padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAction(icon = KaisouIcons.Back, contentDescription = stringResource(R.string.cd_back), onClick = onBack)
            Text(
                text = user?.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.spaceSmall)
                    .alpha(barProgress)
            )

            // The button that puts what they say of themselves in the app language, on until turned off, at the start of the share
            if (user != null && user.about.isNotBlank() && AppLanguage.needsTranslation) {
                HeaderAction(
                    icon = KaisouIcons.Translate,
                    contentDescription = stringResource(R.string.title_translate),
                    onClick = viewModel::toggleTranslate,
                    active = translate
                )
            }
            user?.siteUrl?.let { url ->
                HeaderAction(
                    icon = KaisouIcons.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                        context.startActivity(Intent.createChooser(send, null))
                    }
                )
            }
        }
    }
}

// What a section that is not made yet says
@Composable
private fun SoonBlock() {
    val dimens = KaisouTheme.dimens

    Text(
        text = stringResource(R.string.user_tab_soon),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceLarge)
    )
}

// The banner, fading into the page at its foot, with the picture, name, badge and follow button, under the room of the status bar
@Composable
private fun UserHeader(user: UserUi, counts: Pair<Int, Int>?, bannerTop: Dp, onFollow: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val whole = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(UserDimens.BannerHeight + bannerTop)) {
            user.banner?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to Color.Transparent, UserDimens.BannerClear to Color.Transparent, 1f to MaterialTheme.colorScheme.background))
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = dimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The picture has no gray under it, as it came
                Box(modifier = Modifier.size(UserDimens.Avatar).clip(CircleShape)) {
                    user.avatar?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    // Held, the name says the ones the person had before, as AniList does, and the follow button ends the line
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                        Box(modifier = Modifier.weight(1f)) {
                            val name = @Composable {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )
                            }
                            if (user.previousNames.isEmpty()) {
                                name()
                            } else {
                                TagTip(stringResource(R.string.user_previous_names, user.previousNames.joinToString(", ")), content = name, translate = false)
                            }
                        }
                        if (!user.isSelf) FollowButton(following = user.isFollowing, onClick = onFollow)
                    }

                    // The year they joined, and what else is known of them in the same line
                    val facts = listOfNotNull(
                        user.joinedYear?.let { stringResource(R.string.user_joined, it.toString()) },
                        stringResource(R.string.user_follows_you).takeIf { user.isFollower && !user.isSelf }
                    )
                    if (facts.isNotEmpty()) {
                        Text(
                            text = facts.joinToString(stringResource(R.string.home_card_separator)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // How many follow them and how many they follow, small, once they are known
                    counts?.let { (followers, following) ->
                        Text(
                            text = stringResource(
                                R.string.user_follow_counts,
                                whole.format(following),
                                pluralStringResource(R.plurals.user_followers, followers, whole.format(followers))
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    user.badge?.let { Chip(text = it, textColor = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

// The button to follow: a rounded rectangle in the red of social follow buttons, with a person and a plus or a tick, and the words
@Composable
private fun FollowButton(following: Boolean, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val content = KaisouTheme.colors.onAccentFill

    Row(
        modifier = Modifier
            .height(UserDimens.FollowSize)
            .clip(MaterialTheme.shapes.small)
            .background(KaisouTheme.colors.follow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = if (following) KaisouIcons.UserCheck else KaisouIcons.UserPlus, contentDescription = null, tint = content, modifier = Modifier.size(UserDimens.FollowIcon))
        Text(
            text = stringResource(if (following) R.string.user_following else R.string.user_follow),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = content,
            maxLines = 1
        )
    }
}
