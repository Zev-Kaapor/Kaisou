package com.kaisou.app.features.title.ui

// Android framework
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.navigation.LocalOpenUser
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.EditorTexts
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.home.ui.components.SpoilerText
import com.kaisou.app.features.home.ui.components.StatusEditor
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.ThreadBody
import com.kaisou.app.features.title.data.ThreadComment
import com.kaisou.app.features.title.data.LinkedTitle
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.DetailsSkeleton
import com.kaisou.app.features.title.ui.components.ForumText
import com.kaisou.app.features.title.ui.components.RoundImage
import com.kaisou.app.features.title.ui.components.relativeTime
import com.kaisou.app.features.title.ui.components.ThreadTags

// A thread read whole: tags, title, author, text, and the comments with their answers a page at a time, with the buttons under each
@Composable
fun ThreadScreen(onBack: () -> Unit, onOpenTitle: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: ThreadViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)
    val loaded = (state.thread as? Load.Done)?.value

    // The comments and their answers in one list, each with how deep it is
    val rows = remember(state.comments) { state.comments.flatMap(::flatten) }
    val copied = stringResource(R.string.thread_copied)
    val copy: (String) -> Unit = { text ->
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text))
        Toasts.show(copied)
    }

    // The next page of the comments is asked for when the last rows are on the screen
    LaunchedEffect(listState, rows.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (loaded != null && last >= rows.size - LoadAhead) viewModel.loadMoreComments()
        }
    }

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
                text = stringResource(R.string.title_thread),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceSmall)
            )
            if (loaded != null) {
                HeaderAction(
                    icon = KaisouIcons.Translate,
                    contentDescription = stringResource(R.string.title_translate),
                    onClick = viewModel::toggleTranslate,
                    active = state.translate
                )
                HeaderAction(
                    icon = KaisouIcons.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, loaded.thread.url)
                        context.startActivity(Intent.createChooser(send, null))
                    }
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollIndicator(listState),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            when {
                loaded != null -> {
                    item(key = "thread") {
                        val shown = if (state.translate) state.translated else null
                        val text = shown?.text ?: loaded.text
                        ThreadContent(
                            thread = loaded,
                            title = shown?.title ?: loaded.thread.title,
                            text = text,
                            cards = state.cards,
                            onOpenTitle = onOpenTitle,
                            onLike = viewModel::toggleLike,
                            onReply = { viewModel.openComposer(null) },
                            onFollow = viewModel::toggleFollow,
                            onCopy = { copy(text) },
                            onReport = { InAppBrowser.open(context, loaded.thread.url) }
                        )
                    }
                    item(key = "comments-title") {
                        Text(
                            text = stringResource(R.string.title_thread_comments),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = dimens.spaceSmall)
                        )
                    }
                    items(rows.size, key = { rows[it].first.id }) { index ->
                        val (comment, depth) = rows[index]
                        CommentRow(
                            comment = comment,
                            depth = depth,
                            first = index == 0,
                            cards = state.cards,
                            onOpenTitle = onOpenTitle,
                            text = if (state.translate) state.commentTexts[comment.id] ?: comment.text else comment.text,
                            onLike = { viewModel.toggleCommentLike(comment.id) },
                            onReply = if (loaded.locked) null else ({ viewModel.openComposer(comment.id) }),
                            onCopy = { copy(comment.text) },
                            onReport = { InAppBrowser.open(context, "${loaded.thread.url}/comment/${comment.id}") }
                        )
                    }
                    when {
                        state.commentsFailed -> item(key = "comments-error") { DetailsError(onRetry = viewModel::loadMoreComments) }
                        state.loadingComments || state.hasMore -> item(key = "comments-loading") { DetailsSkeleton() }
                        rows.isEmpty() -> item(key = "comments-empty") {
                            Text(
                                text = stringResource(R.string.title_thread_no_comments),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                state.thread is Load.Failed -> item(key = "error") { DetailsError(onRetry = viewModel::load) }
                else -> item(key = "loading") { DetailsSkeleton() }
            }
        }
    }

    // The screen of writing a comment, over the thread
    state.composer?.let { composer ->
        StatusEditor(composer = composer.state, texts = CommentTexts, onPost = viewModel::postComment, onClose = viewModel::closeComposer)
    }
}

// How many rows before the end of the list the next page of comments is asked for
private const val LoadAhead = 4

// What the screen of writing says when it is a comment
private val CommentTexts = EditorTexts(R.string.comment_title, R.string.comment_hint, R.string.status_confirm_title, R.string.comment_confirm_text, R.string.comment_failed)

// A comment and, after it, the answers to it, each with how deep it is
private fun flatten(comment: ThreadComment, depth: Int = 0): List<Pair<ThreadComment, Int>> =
    listOf(comment to depth) + comment.replies.flatMap { flatten(it, depth + 1) }

// The tags, the author with the picture and the day, then the title, the text, and the buttons
@Composable
private fun ThreadContent(
    thread: ThreadBody,
    title: String,
    text: String,
    cards: Map<Int, LinkedTitle>,
    onOpenTitle: (Int) -> Unit,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onFollow: () -> Unit,
    onCopy: () -> Unit,
    onReport: () -> Unit
) {
    val dimens = KaisouTheme.dimens
    val day = relativeTime(thread.thread.createdAt)
    val openUser = LocalOpenUser.current

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)) {
        if (thread.tags.isNotEmpty()) ThreadTags(tags = thread.tags, onOpenTitle = onOpenTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
            // The picture and the name of the author open their page
            Row(
                modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button) { openUser(thread.thread.author) },
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundImage(url = thread.thread.avatar, size = TitleDimens.ReviewReaderAvatar)
                Column {
                    Text(text = thread.thread.author, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(text = day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ActionButton(
                icon = if (thread.subscribed) KaisouIcons.BellRinging else KaisouIcons.Bell,
                description = stringResource(if (thread.subscribed) R.string.cd_thread_unfollow else R.string.cd_thread_follow),
                onClick = onFollow,
                active = thread.subscribed
            )
        }
        SpoilerText(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            linkColor = MaterialTheme.colorScheme.primary,
            headingSize = MaterialTheme.typography.titleSmall.fontSize
        )
        ForumText(text = text, cards = cards, onOpenTitle = onOpenTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            MoreMenu(onCopy = onCopy, onReport = onReport)
            if (!thread.locked) ActionButton(KaisouIcons.Reply, stringResource(R.string.cd_thread_reply), onReply)
            LikeButton(liked = thread.liked, count = thread.thread.likes, onClick = onLike)
        }
    }
}

// A comment of the thread with no card: author, day, text and buttons, an answer moved in with a line at its side
@Composable
private fun CommentRow(
    comment: ThreadComment,
    depth: Int,
    first: Boolean,
    cards: Map<Int, LinkedTitle>,
    onOpenTitle: (Int) -> Unit,
    text: String,
    onLike: () -> Unit,
    onReply: (() -> Unit)?,
    onCopy: () -> Unit,
    onReport: () -> Unit
) {
    val dimens = KaisouTheme.dimens
    val day = relativeTime(comment.createdAt)
    val openUser = LocalOpenUser.current

    Column(modifier = Modifier.fillMaxWidth()) {
        if (depth == 0 && !first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(bottom = dimens.spaceMedium))
        Row(modifier = Modifier.fillMaxWidth().padding(start = TitleDimens.CommentIndent * depth.coerceAtMost(MaxDepth)).height(IntrinsicSize.Min)) {
            if (depth > 0) {
                Box(modifier = Modifier.width(TitleDimens.CommentLine).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(modifier = Modifier.width(dimens.spaceMedium))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
                    RoundImage(url = comment.avatar, size = TitleDimens.CommentAvatar, modifier = Modifier.clickable(role = Role.Button) { openUser(comment.author) })
                    Text(
                        text = comment.author,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).clickable(role = Role.Button) { openUser(comment.author) }
                    )
                    Text(text = day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                ForumText(text = text, cards = cards, onOpenTitle = onOpenTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    MoreMenu(onCopy = onCopy, onReport = onReport)
                    onReply?.let { ActionButton(KaisouIcons.Reply, stringResource(R.string.cd_thread_reply), it) }
                    LikeButton(liked = comment.liked, count = comment.likes, onClick = onLike)
                }
            }
        }
    }
}

// The heart that is filled and in the accent when the user gave one, with how many people gave one
@Composable
internal fun LikeButton(liked: Boolean, count: Int, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = dimens.spaceExtraSmall, horizontal = dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (liked) KaisouIcons.HeartFilled else KaisouIcons.Heart,
            contentDescription = stringResource(R.string.cd_thread_like),
            tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TitleDimens.ThreadActionIcon)
        )
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// The three dots that open the menu of what is done less: copy the text and report
@Composable
private fun MoreMenu(onCopy: () -> Unit, onReport: () -> Unit) {
    var open by remember { mutableStateOf(false) }

    Box {
        ActionButton(KaisouIcons.More, stringResource(R.string.cd_thread_more), { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            MenuLine(KaisouIcons.Copy, R.string.cd_thread_copy) {
                open = false
                onCopy()
            }
            MenuLine(KaisouIcons.Flag, R.string.cd_thread_report) {
                open = false
                onReport()
            }
        }
    }
}

// A line of a menu: its icon and its name
@Composable
private fun MenuLine(icon: ImageVector, label: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.ListIcon)) },
        text = { Text(text = stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground) },
        onClick = onClick
    )
}

// A round button with an icon, in the accent when it is on
@Composable
internal fun ActionButton(icon: ImageVector, description: String, onClick: () -> Unit, active: Boolean = false) {
    Box(
        modifier = Modifier
            .size(TitleDimens.ThreadAction)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TitleDimens.ThreadActionIcon)
        )
    }
}

// The answers are not moved in further than this
private const val MaxDepth = 3
