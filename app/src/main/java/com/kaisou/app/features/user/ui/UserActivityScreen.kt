package com.kaisou.app.features.user.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Material 3 components
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.designsystem.components.ConfirmDialog
import com.kaisou.app.core.designsystem.components.clickableNoIndication
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.navigation.LocalOpenUser
import com.kaisou.app.features.home.data.ReplyUi
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.home.ui.components.RepliesSheet
import com.kaisou.app.features.home.ui.components.ReplyMenu
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.user.ui.components.UserActivitySkeleton
import com.kaisou.app.features.user.ui.components.ActivityActions
import com.kaisou.app.features.user.ui.components.UserActivityCard

// Everything a person did lately, a page at a time: the next one is asked for when the list gets near its end. A title opens its page, and
// each activity has the bell, the menu, the comments and the heart
@Composable
fun UserActivityScreen(onBack: () -> Unit, onOpenTitle: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: UserActivityViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)
    val openUser = LocalOpenUser.current
    var toDelete by remember { mutableStateOf<Int?>(null) }
    var replyMenu by remember { mutableStateOf<ReplyUi?>(null) }
    var replyToDelete by remember { mutableStateOf<ReplyUi?>(null) }
    val ownUserId = AniListSession.viewer?.id
    val actions = remember(viewModel) {
        ActivityActions(
            isOwn = viewModel.isOwn,
            onLike = viewModel::toggleLike,
            onFollow = viewModel::toggleFollow,
            onOpenReplies = viewModel::openReplies,
            onDelete = { toDelete = it }
        )
    }

    // The next page is asked for when the last rows are on the screen
    LaunchedEffect(listState, state.items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= state.items.size - LoadAhead) viewModel.loadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderAction(icon = KaisouIcons.Back, contentDescription = stringResource(R.string.cd_back), onClick = onBack)
                Text(
                    text = stringResource(R.string.user_section_recent),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = dimens.spaceSmall)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().scrollIndicator(listState),
                contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                items(state.items, key = { it.id }) { item -> UserActivityCard(item, onOpenTitle, actions = actions) }
                // The placeholder of the first page and the one of the next pages have different keys, so what comes in before the
                // second does not carry the list down with it
                when {
                    state.failed -> item(key = "error") { DetailsError(onRetry = viewModel::loadMore) }
                    state.loading || state.hasMore -> item(key = if (state.items.isEmpty()) "loading-first" else "loading-more") {
                        UserActivitySkeleton(cards = if (state.items.isEmpty()) SkeletonFirst else SkeletonMore)
                    }
                }
            }
        }

        // The comments of an activity rise from the bottom, and the menu of a comment and the questions come over them
        state.replies?.let { replies ->
            RepliesSheet(
                replies = replies,
                onSend = viewModel::sendReply,
                onLike = viewModel::toggleReplyLike,
                onMenu = { replyMenu = it },
                onProfile = { openUser(it.userName) },
                onCancelEdit = viewModel::cancelEditingReply,
                onDismiss = viewModel::closeReplies,
                modifier = Modifier.clickableNoIndication {}
            )
        }
        replyMenu?.let { reply ->
            ReplyMenu(
                reply = reply,
                activityId = state.replies?.activityId ?: 0,
                isOwn = reply.userId == ownUserId,
                onEdit = { viewModel.startEditingReply(reply.id) },
                onDelete = { replyToDelete = reply },
                onDismiss = { replyMenu = null }
            )
        }
        replyToDelete?.let { reply ->
            ConfirmDialog(
                title = stringResource(R.string.reply_delete_title),
                text = stringResource(R.string.reply_delete_text),
                confirmLabel = stringResource(R.string.story_delete_confirm),
                cancelLabel = stringResource(R.string.action_cancel),
                onConfirm = {
                    replyToDelete = null
                    viewModel.deleteReply(reply.id)
                },
                onDismiss = { replyToDelete = null }
            )
        }
        toDelete?.let { id ->
            ConfirmDialog(
                title = stringResource(R.string.story_delete_title),
                text = stringResource(R.string.story_delete_text),
                confirmLabel = stringResource(R.string.story_delete_confirm),
                cancelLabel = stringResource(R.string.action_cancel),
                onConfirm = {
                    toDelete = null
                    viewModel.deleteActivity(id)
                },
                onDismiss = { toDelete = null }
            )
        }
    }
}

// How many rows before the end of the list the next page is asked for, and how many placeholders stand in for the first page and for the next ones
private const val LoadAhead = 4
private const val SkeletonFirst = 6
private const val SkeletonMore = 2
