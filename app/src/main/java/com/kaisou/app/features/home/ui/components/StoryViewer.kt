package com.kaisou.app.features.home.ui.components

// Android text
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.text.format.DateUtils
// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// Image loading
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.animation.core.animate
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.ConfirmDialog
import com.kaisou.app.core.designsystem.components.ImmersiveDialogEffect
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.components.MenuCard
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.SheetCloseButton
import com.kaisou.app.core.designsystem.components.clickableNoIndication
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.RepliesUi
import com.kaisou.app.features.home.data.ReplyUi
import com.kaisou.app.features.home.data.StoryItemUi
import com.kaisou.app.features.home.data.StoryUi
import com.kaisou.app.features.home.ui.HomeDimens

// The stories of the people, full screen: one activity at a time over its picture, with a bar for each of them on top that fills as
// time goes. A tap on the right goes forward and on the left goes back, in the middle it opens the title, holding stops the time. When the last one of a person ends
// the next person starts, and after the last person it closes. Each activity shown is marked as seen
@Composable
fun StoryViewer(
    stories: List<StoryUi>,
    startUser: Int,
    startItem: Int?,
    onPosition: (Int, Int) -> Unit,
    ownUserId: Int,
    seen: Set<Int>,
    replies: RepliesUi?,
    onDelete: (Int) -> Unit,
    onSeen: (Int) -> Unit,
    onLike: (Int) -> Unit,
    onOpenReplies: (Int) -> Unit,
    onLikeReply: (Int) -> Unit,
    onDeleteReply: (Int) -> Unit,
    onEditReply: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onSendReply: (String) -> Unit,
    onCloseReplies: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (replies != null) onCloseReplies() else onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ImmersiveDialogEffect()
        StoryPlayer(
            stories = stories,
            startUser = startUser,
            startItem = startItem,
            onPosition = onPosition,
            ownUserId = ownUserId,
            seen = seen,
            replies = replies,
            onDelete = onDelete,
            onSeen = onSeen,
            onLike = onLike,
            onOpenReplies = onOpenReplies,
            onLikeReply = onLikeReply,
            onDeleteReply = onDeleteReply,
            onEditReply = onEditReply,
            onCancelEdit = onCancelEdit,
            onSendReply = onSendReply,
            onCloseReplies = onCloseReplies,
            onOpenTitle = onOpenTitle,
            onClose = onClose
        )
    }
}

@Composable
private fun StoryPlayer(
    stories: List<StoryUi>,
    startUser: Int,
    startItem: Int?,
    onPosition: (Int, Int) -> Unit,
    ownUserId: Int,
    seen: Set<Int>,
    replies: RepliesUi?,
    onDelete: (Int) -> Unit,
    onSeen: (Int) -> Unit,
    onLike: (Int) -> Unit,
    onOpenReplies: (Int) -> Unit,
    onLikeReply: (Int) -> Unit,
    onDeleteReply: (Int) -> Unit,
    onEditReply: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onSendReply: (String) -> Unit,
    onCloseReplies: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onClose: () -> Unit
) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    // Lying down the window is wider than it is tall
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    // The person and the activity that are on are told to the screen as they change, which keeps them when the phone is turned, and the next
    // time this is made it starts from them
    var userIndex by remember { mutableIntStateOf(startUser) }
    val user = stories[userIndex]
    // A person starts at the first activity not seen yet, or at the first one if all were seen, or at the one the screen says when it is there
    fun firstOf(person: StoryUi) = person.items.indexOfFirst { it.id !in seen }.coerceAtLeast(0)
    fun startOf(person: StoryUi) = person.items.indexOfFirst { it.id == startItem }.takeIf { it >= 0 } ?: firstOf(person)
    var itemIndex by remember(userIndex) { mutableIntStateOf(startOf(user)) }
    val item = user.items[itemIndex]
    LaunchedEffect(userIndex, itemIndex) { onPosition(user.userId, item.id) }
    var held by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    // The comment whose menu is open, and the one the user is asked whether to delete
    var replyMenu by remember { mutableStateOf<ReplyUi?>(null) }
    var replyToDelete by remember { mutableStateOf<ReplyUi?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // The cube that turns from a person to another: where it goes and which way, how far it has turned from nothing to all, and the movement
    // that takes it to its end
    var turn by remember { mutableStateOf<StoryTurn?>(null) }
    var turned by remember { mutableFloatStateOf(0f) }
    var turning by remember { mutableStateOf<Job?>(null) }
    // The time also stops while the comments or the menu are open, while the user is asked whether to delete and while the cube turns
    val paused = held || replies != null || menuOpen || confirmingDelete || turn != null
    val isOwn = user.userId == ownUserId
    var restarts by remember { mutableIntStateOf(0) }
    val progress = remember(userIndex, itemIndex, restarts) { Animatable(0f) }
    val duration = if (item.text != null) HomeDimens.StoryTextMillis else HomeDimens.StoryItemMillis

    // The pictures of what comes next, the next activity of the person or the first of the next person, are asked for before they are needed, so
    // the change finds them and shows them at once
    LaunchedEffect(userIndex, itemIndex) {
        val following = user.items.getOrNull(itemIndex + 1) ?: stories.getOrNull(userIndex + 1)?.let { it.items[firstOf(it)] }
        val loader = SingletonImageLoader.get(context)
        listOfNotNull(following?.backdropUrl, following?.coverUrl).forEach { loader.enqueue(ImageRequest.Builder(context).data(it).build()) }
    }

    // Takes the cube from where it is to its end: all the way turned to the other person, or back to the one it came from
    fun settleTurn(commit: Boolean) {
        val current = turn ?: return
        turning?.cancel()
        turning = scope.launch {
            animate(turned, if (commit) 1f else 0f, animationSpec = tween(HomeDimens.StoryTurnMillis)) { value, _ -> turned = value }
            if (commit) userIndex = current.to
            turn = null
            turned = 0f
        }
    }

    fun startTurn(to: Int, forward: Boolean) {
        if (turn != null) return
        turned = 0f
        turn = StoryTurn(to, forward)
        settleTurn(commit = true)
    }

    // A turn that is left with nothing taking it to its end, which a drag that was cut could do, is undone, so it never holds the touches
    fun clearStaleTurn(): Boolean {
        if (turn == null) return false
        if (turning?.isActive == true) return true
        turn = null
        turned = 0f
        return false
    }

    fun next() {
        if (clearStaleTurn()) return
        when {
            itemIndex < stories[userIndex].items.lastIndex -> itemIndex++
            userIndex < stories.lastIndex -> startTurn(userIndex + 1, forward = true)
            else -> onClose()
        }
    }

    // Going back from the first activity of the first person starts it again
    fun previous() {
        if (clearStaleTurn()) return
        when {
            itemIndex > 0 -> itemIndex--
            userIndex > 0 -> startTurn(userIndex - 1, forward = false)
            else -> restarts++
        }
    }

    // The time of an activity fills its bar, and when it is full the next one comes. Holding stops it where it is
    LaunchedEffect(userIndex, itemIndex, restarts, paused) {
        onSeen(item.id)
        if (!paused) {
            progress.animateTo(1f, tween((duration * (1f - progress.value)).toInt(), easing = LinearEasing))
            next()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.veil)
            .pointerInput(userIndex) {
                // One gesture for the whole screen, so a touch is only ever taken by it: the time stops while a finger is down. A short touch
                // that stayed where it was goes back on the left third, forward on the right third and, in the middle, to the title of the
                // activity when it has one, forward when it has none. A long one only held the time, so letting go goes on where it was and does
                // not change the activity. Any drag to the side goes to another person, however short: going left to the next one, or closing
                // when there is none, and going right to the one before, or starting again when there is none. The cube turns with the finger
                // and then goes on by itself. It is made again for each person, since the activity of each one is a state of its own, and a
                // gesture that was made for the one before it would change that one's
                awaitEachGesture {
                    val down = awaitFirstDown()
                    held = true
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    var cancelled = false
                    var moved = 0f
                    var lifted: PointerInputChange? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            cancelled = true
                            break
                        }
                        if (change.changedToUpIgnoreConsumed()) {
                            lifted = change
                            break
                        }
                        // Something else took the touch, as a button does
                        if (!dragging && change.isConsumed) {
                            cancelled = true
                            break
                        }
                        moved = change.position.x - down.position.x
                        if (!dragging && abs(moved) > slop) {
                            dragging = true
                            turning?.cancel()
                        }
                        if (dragging) {
                            change.consume()
                            val forward = moved < 0f
                            val to = userIndex + if (forward) 1 else -1
                            if (to in stories.indices) {
                                if (turn?.forward != forward) turn = StoryTurn(to, forward)
                                turned = (abs(moved) / size.width).coerceIn(0f, 1f)
                            } else if (turn != null) {
                                turn = null
                                turned = 0f
                            }
                        }
                    }
                    held = false
                    if (dragging) {
                        val forward = moved < 0f
                        val to = userIndex + if (forward) 1 else -1
                        when {
                            cancelled -> if (turn != null) settleTurn(commit = false)
                            to in stories.indices -> {
                                if (turn?.forward != forward) turn = StoryTurn(to, forward)
                                settleTurn(commit = true)
                            }
                            forward -> onClose()
                            else -> {
                                // The first person has nobody before, so it starts again from its first activity
                                turn = null
                                turned = 0f
                                itemIndex = 0
                                restarts++
                            }
                        }
                    } else if (lifted != null && lifted.uptimeMillis - down.uptimeMillis < HoldMillis) {
                        val place = down.position.x / size.width
                        val title = stories[userIndex].items[itemIndex].mediaId
                        when {
                            place < HomeDimens.StoryTapFraction -> previous()
                            place > 1f - HomeDimens.StoryTapFraction || title == null -> next()
                            else -> onOpenTitle(title)
                        }
                    }
                }
            }
    ) {
        val moving = turn
        if (moving == null) {
            StoryFace(
                user = user,
                item = item,
                itemIndex = itemIndex,
                progress = { progress.value },
                landscape = landscape,
                onMenu = { menuOpen = true },
                onClose = onClose,
                onOpenReplies = onOpenReplies,
                onLike = onLike,
                onOpenProfile = { InAppBrowser.open(context, UserUrl + user.name) }
            )
        } else {
            // While the cube turns the person that goes out and the one that comes in are both drawn, each on its side of the corner, and
            // nothing on them answers to a touch
            val coming = stories[moving.to]
            val comingIndex = firstOf(coming)
            StoryFace(
                user = user,
                item = item,
                itemIndex = itemIndex,
                progress = { progress.value },
                landscape = landscape,
                onMenu = {},
                onClose = {},
                onOpenReplies = {},
                onLike = {},
                onOpenProfile = {},
                modifier = Modifier.cube(outgoing = true, forward = moving.forward) { turned }
            )
            StoryFace(
                user = coming,
                item = coming.items[comingIndex],
                itemIndex = comingIndex,
                progress = { 0f },
                landscape = landscape,
                onMenu = {},
                onClose = {},
                onOpenReplies = {},
                onLike = {},
                onOpenProfile = {},
                modifier = Modifier.cube(outgoing = false, forward = moving.forward) { turned }
            )
        }

        // The options of the activity: sharing it, opening it on AniList, copying its text and, when it is the user's own, deleting it
        if (menuOpen) {
            StoryMenu(
                user = user,
                item = item,
                isOwn = isOwn,
                onDelete = { confirmingDelete = true },
                onDismiss = { menuOpen = false }
            )
        }

        if (confirmingDelete) {
            ConfirmDialog(
                title = stringResource(R.string.story_delete_title),
                text = stringResource(R.string.story_delete_text),
                confirmLabel = stringResource(R.string.story_delete_confirm),
                cancelLabel = stringResource(R.string.action_cancel),
                onConfirm = {
                    confirmingDelete = false
                    onDelete(item.id)
                    onClose()
                },
                onDismiss = { confirmingDelete = false }
            )
        }

        // The comments rise from the bottom and slide out when closed, the sheet of the app darkens what is behind it
        if (replies != null) {
            RepliesSheet(
                replies = replies,
                onSend = onSendReply,
                onLike = onLikeReply,
                onMenu = { replyMenu = it },
                // The profile is not in the app yet, so for now the name and the picture open the page of the person on AniList
                onProfile = { InAppBrowser.open(context, UserUrl + it.userName) },
                onCancelEdit = onCancelEdit,
                onDismiss = onCloseReplies,
                modifier = Modifier.clickableNoIndication {}
            )
        }
        replyMenu?.let { reply ->
            ReplyMenu(
                reply = reply,
                activityId = replies?.activityId ?: item.id,
                isOwn = reply.userId == ownUserId,
                onEdit = { onEditReply(reply.id) },
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
                    onDeleteReply(reply.id)
                },
                onDismiss = { replyToDelete = null }
            )
        }
    }
}

// Where the cube goes: the person it turns to and whether it is forward, to the one after
private class StoryTurn(val to: Int, val forward: Boolean)

// What a person shows at a moment: the picture of the title blurred behind, the bars of their activities with the one that is on filling, who
// they are with the time since they did it, and the activity. The picture and the name open the person. A new activity comes in with a fade
// and a rise
@Composable
private fun StoryFace(
    user: StoryUi,
    item: StoryItemUi,
    itemIndex: Int,
    progress: () -> Float,
    landscape: Boolean,
    onMenu: () -> Unit,
    onClose: () -> Unit,
    onOpenReplies: (Int) -> Unit,
    onLike: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    // What the activity shows comes in with a short fade and a rise each time it changes, in one copy and not over the old one, so nothing
    // blinks while a picture loads. The one the face starts with is already there
    val firstId = remember { item.id }
    val enter = remember(item.id) { Animatable(if (item.id == firstId) 1f else 0f) }
    LaunchedEffect(item.id) { if (enter.value < 1f) enter.animateTo(1f, tween(HomeDimens.StoryFadeMillis)) }
    val entering = Modifier.graphicsLayer {
        alpha = enter.value
        translationY = (1f - enter.value) * HomeDimens.StoryRise.toPx()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // The picture of the title, blurred and dark, behind everything
        AsyncImage(
            model = item.backdropUrl ?: item.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(HomeDimens.StoryBackdropBlur)
        )
        Box(modifier = Modifier.fillMaxSize().background(colors.veil.copy(alpha = HomeDimens.StoryBackdropAlpha)))

        Column(
            // The keyboard is left out of the room the story keeps clear, it is the sheet of the comments that makes way for it, so the cover
            // does not shrink when the keyboard comes and grow when it goes
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)).padding(horizontal = dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(HomeDimens.StoryBarGap)
            ) {
                user.items.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(HomeDimens.StoryBarHeight)
                            .clip(CircleShape)
                            .background(colors.onMedia.copy(alpha = HomeDimens.StoryBarTrackAlpha))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = when {
                                        index < itemIndex -> 1f
                                        index == itemIndex -> progress()
                                        else -> 0f
                                    }
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                }
                                .background(colors.onMedia)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)) {
                // The picture and the name of the person, which open their page
                Row(
                    modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.large).clickable(onClick = onOpenProfile),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
                ) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(HomeDimens.StoryHeaderAvatar).clip(CircleShape).background(colors.skeleton)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onMedia,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = DateUtils.getRelativeTimeSpanString(item.createdAt * MillisPerSecond, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // The three dots, with the options for the activity that is on, and the X, close to each other
                Row(horizontalArrangement = Arrangement.spacedBy(HomeDimens.StoryHeaderActionsGap), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = KaisouIcons.More,
                        contentDescription = stringResource(R.string.story_more),
                        tint = colors.onMedia,
                        modifier = Modifier.size(dimens.buttonHeight).clip(CircleShape).clickable(onClick = onMenu).padding(dimens.spaceMedium)
                    )
                    Icon(
                        imageVector = KaisouIcons.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = colors.onMedia,
                        modifier = Modifier.size(dimens.buttonHeight).clip(CircleShape).clickable(onClick = onClose).padding(dimens.spaceMedium)
                    )
                }
            }
            if (landscape && item.text == null) {
                // Lying down the cover goes on the left, as tall as the room is, and the line and the buttons go on the right
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceLarge)) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(HomeDimens.PosterRatio)
                            .then(entering)
                            .clip(MaterialTheme.shapes.large)
                            .background(colors.skeleton)
                    )
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Box(modifier = entering) { StoryLine(item = item, textAlign = TextAlign.Start) }
                        }
                        StoryActions(item = item, onOpenReplies = onOpenReplies, onLike = onLike)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().then(entering), contentAlignment = Alignment.Center) {
                    if (item.text != null) {
                        MarkdownContent(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onMedia,
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spaceLarge)) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth(HomeDimens.StoryCoverFraction)
                                    .aspectRatio(HomeDimens.PosterRatio)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(colors.skeleton)
                            )
                            StoryLine(item = item, textAlign = TextAlign.Center)
                        }
                    }
                }
                StoryActions(item = item, onOpenReplies = onOpenReplies, onLike = onLike)
            }
        }
    }
}

// A face of the cube while it turns, from nothing to all: the one that goes out turns on its edge toward the side it leaves by, and the one that
// comes in turns from the side it comes from, so both meet in a corner. The one that is turned away gets dark
private fun Modifier.cube(outgoing: Boolean, forward: Boolean, turned: () -> Float): Modifier =
    graphicsLayer {
        val amount = turned()
        val side = if (forward) 1f else -1f
        cameraDistance = HomeDimens.StoryCubeCamera * density
        if (outgoing) {
            transformOrigin = TransformOrigin(if (forward) 1f else 0f, 0.5f)
            rotationY = -90f * side * amount
            translationX = -side * size.width * amount
        } else {
            transformOrigin = TransformOrigin(if (forward) 0f else 1f, 0.5f)
            rotationY = 90f * side * (1f - amount)
            translationX = side * size.width * (1f - amount)
        }
    }.drawWithContent {
        drawContent()
        drawRect(Color.Black.copy(alpha = HomeDimens.StoryCubeShade * if (outgoing) turned() else 1f - turned()))
    }

// What the person did, with the episodes or the chapters in the violet of the app
@Composable
private fun StoryLine(item: StoryItemUi, textAlign: TextAlign) {
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            append(item.line.orEmpty())
            item.highlight?.let { addStyle(SpanStyle(color = accent), it.first, it.last + 1) }
        },
        style = MaterialTheme.typography.titleSmall,
        color = KaisouTheme.colors.onMedia,
        textAlign = textAlign
    )
}

// The bar to write a comment, and at its side the bubble of a conversation and the heart, each with how many there are under it
@Composable
private fun StoryActions(item: StoryItemUi, onOpenReplies: (Int) -> Unit, onLike: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceSmall),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(dimens.buttonHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { onOpenReplies(item.id) }
                .padding(horizontal = dimens.spaceLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.story_reply_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StackedAction(count = item.replyCount, onClick = { onOpenReplies(item.id) }) {
            Icon(
                imageVector = KaisouIcons.Comment,
                contentDescription = stringResource(R.string.story_replies_title),
                tint = KaisouTheme.colors.onMedia
            )
        }
        LikeButton(liked = item.isLiked, count = item.likeCount, key = item.id, onClick = { onLike(item.id) })
    }
}

// A button with its icon and, under it, how many there are, when there is any. The icons of two of them stay on the same line whether
// they have a number or not
@Composable
private fun StackedAction(count: Int, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
    ) {
        icon()
        CountText(count)
    }
}

// The menu of an activity in the sheet of the app, as the menus of Wavvy: who did it and when on top, and each option as a card
@Composable
private fun StoryMenu(user: StoryUi, item: StoryItemUi, isOwn: Boolean, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    val context = LocalContext.current
    val url = ActivityUrl + item.id

    KaisouSheet(onDismiss = onDismiss, modifier = Modifier.clickableNoIndication {}) {
        // Every option slides the menu out and then does what it says
        val closeSheet = LocalSheetClose.current
        val run: (() -> Unit) -> () -> Unit = { action ->
            {
                closeSheet()
                action()
            }
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = dimens.screenPadding).padding(bottom = dimens.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.MenuCardGap)
        ) {
            Row(
                modifier = Modifier.padding(bottom = dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(HomeDimens.StoryHeaderAvatar).clip(CircleShape).background(colors.skeleton)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(item.createdAt * MillisPerSecond, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MenuCard(
                icon = KaisouIcons.Share,
                title = stringResource(R.string.story_share),
                onClick = run {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    context.startActivity(Intent.createChooser(send, null))
                }
            )
            MenuCard(icon = KaisouIcons.ExternalLink, title = stringResource(R.string.story_open_anilist), onClick = run { InAppBrowser.open(context, url) })
            item.text?.let { text ->
                MenuCard(
                    icon = KaisouIcons.Copy,
                    title = stringResource(R.string.story_copy_text),
                    onClick = run { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text)) }
                )
            }
            if (isOwn) {
                MenuCard(
                    icon = KaisouIcons.Trash,
                    title = stringResource(R.string.story_delete),
                    tint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = run(onDelete)
                )
            }
        }
    }
}

// The comments of an activity in the sheet of the app: who wrote each one and when, and where the user writes theirs. The keyboard
// lifts the sheet, and a comment that comes in slides into the list
@Composable
private fun RepliesSheet(
    replies: RepliesUi,
    onSend: (String) -> Unit,
    onLike: (Int) -> Unit,
    onMenu: (ReplyUi) -> Unit,
    onProfile: (ReplyUi) -> Unit,
    onCancelEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    var text by remember(replies.activityId) { mutableStateOf("") }
    val canSend = text.isNotBlank() && !replies.sending
    // A comment that was taken leaves the field empty for the next one
    var known by remember(replies.activityId) { mutableIntStateOf(replies.items.size) }
    LaunchedEffect(replies.items.size) {
        if (replies.items.size > known) text = ""
        known = replies.items.size
    }
    // Changing a comment puts its text in the field, and leaving that empties the field again
    LaunchedEffect(replies.editingId) {
        text = replies.editingId?.let { id -> replies.items.firstOrNull { it.id == id }?.text }.orEmpty()
    }

    // The sheet is as tall as a share of the window, and the keyboard only makes it shorter if it does not fit
    val window = LocalWindowInfo.current.containerSize
    val fraction = if (window.width > window.height) HomeDimens.RepliesPanelFractionLandscape else HomeDimens.RepliesPanelFraction
    val sheetHeight = with(LocalDensity.current) { (window.height * fraction).toDp() }

    KaisouSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(modifier = Modifier.height(sheetHeight).padding(horizontal = dimens.screenPadding)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.story_replies_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                SheetCloseButton()
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    replies.loading -> Text(
                        text = stringResource(R.string.story_replies_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    replies.items.isEmpty() -> Text(
                        text = stringResource(R.string.story_replies_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> {
                        val listState = rememberLazyListState()
                        // The newest comment is at the end, and it is where the list stays when one is added
                        LaunchedEffect(replies.items.size) { listState.animateScrollToItem(replies.items.lastIndex) }
                        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(dimens.spaceLarge)) {
                            items(replies.items, key = { it.id }) { reply ->
                                ReplyRow(
                                    reply = reply,
                                    onLike = { onLike(reply.id) },
                                    onMenu = { onMenu(reply) },
                                    onProfile = { onProfile(reply) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
            if (replies.editingId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reply_editing),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = KaisouIcons.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(HomeDimens.ReplyActionIcon).clickable(onClick = onCancelEdit)
                    )
                }
            }
            if (replies.failed) {
                Text(
                    text = stringResource(R.string.story_reply_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = dimens.spaceSmall)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(dimens.buttonHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = dimens.spaceLarge),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.story_reply_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Icon(
                    imageVector = KaisouIcons.Send,
                    contentDescription = stringResource(R.string.story_send),
                    tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(dimens.buttonHeight)
                        .clip(CircleShape)
                        .clickable(enabled = canSend) { onSend(text) }
                        .padding(dimens.spaceMedium)
                )
            }
        }
    }
}

// A comment: the picture and the name, which open the profile of who wrote it, the text, and under it the heart with how many liked it
// and the three dots with the options
@Composable
private fun ReplyRow(reply: ReplyUi, onLike: () -> Unit, onMenu: () -> Unit, onProfile: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)) {
        AsyncImage(
            model = reply.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(HomeDimens.ReplyAvatar).clip(CircleShape).background(colors.skeleton).clickable(onClick = onProfile)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.userName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).clickable(onClick = onProfile)
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(reply.createdAt * MillisPerSecond, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MarkdownContent(text = reply.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(
                modifier = Modifier.padding(top = dimens.spaceExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReplyLike(liked = reply.isLiked, count = reply.likeCount, key = reply.id, onClick = onLike)
                Icon(
                    imageVector = KaisouIcons.More,
                    contentDescription = stringResource(R.string.story_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(HomeDimens.ReplyActionIcon).clip(CircleShape).clickable(onClick = onMenu)
                )
            }
        }
    }
}

// The menu of a comment in the sheet of the app: changing it and deleting it, only for the ones of the user, and for all of them
// copying the text, sharing the activity and opening it on AniList, which has no page of its own for a comment
@Composable
private fun ReplyMenu(reply: ReplyUi, activityId: Int, isOwn: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    val context = LocalContext.current
    val url = ActivityUrl + activityId

    KaisouSheet(onDismiss = onDismiss, modifier = Modifier.clickableNoIndication {}) {
        val closeSheet = LocalSheetClose.current
        val run: (() -> Unit) -> () -> Unit = { action ->
            {
                closeSheet()
                action()
            }
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = dimens.screenPadding).padding(bottom = dimens.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(ComponentDimens.MenuCardGap)
        ) {
            Row(
                modifier = Modifier.padding(bottom = dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                AsyncImage(
                    model = reply.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(HomeDimens.StoryHeaderAvatar).clip(CircleShape).background(colors.skeleton)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reply.userName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(reply.createdAt * MillisPerSecond, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isOwn) MenuCard(icon = KaisouIcons.Edit, title = stringResource(R.string.reply_edit), onClick = run(onEdit))
            MenuCard(
                icon = KaisouIcons.Copy,
                title = stringResource(R.string.story_copy_text),
                onClick = run { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, reply.text)) }
            )
            MenuCard(
                icon = KaisouIcons.Share,
                title = stringResource(R.string.story_share),
                onClick = run {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    context.startActivity(Intent.createChooser(send, null))
                }
            )
            MenuCard(icon = KaisouIcons.ExternalLink, title = stringResource(R.string.story_open_anilist), onClick = run { InAppBrowser.open(context, url) })
            if (isOwn) {
                MenuCard(
                    icon = KaisouIcons.Trash,
                    title = stringResource(R.string.reply_delete),
                    tint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = run(onDelete)
                )
            }
        }
    }
}

// How big a heart is, which pops when the like is given and rests at its size otherwise. Only a like given now pops, not one that was
// already there when the activity or the comment came up
@Composable
private fun rememberLikePop(liked: Boolean, key: Int): Animatable<Float, AnimationVector1D> {
    val scale = remember { Animatable(1f) }
    var wasLiked by remember(key) { mutableStateOf(liked) }

    LaunchedEffect(liked) {
        if (liked && !wasLiked) {
            scale.snapTo(HomeDimens.LikePopFrom)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        wasLiked = liked
    }
    return scale
}

// The heart of an activity: it pops when the like is given, and its color and its count change with a transition
@Composable
private fun LikeButton(liked: Boolean, count: Int, key: Int, onClick: () -> Unit) {
    val colors = KaisouTheme.colors
    val scale = rememberLikePop(liked, key)
    val tint by animateColorAsState(if (liked) MaterialTheme.colorScheme.primary else colors.onMedia, label = "likeTint")

    StackedAction(count = count, onClick = onClick) {
        Icon(
            imageVector = if (liked) KaisouIcons.HeartFilled else KaisouIcons.Heart,
            contentDescription = stringResource(R.string.story_like),
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        )
    }
}

// The heart of a comment, smaller, with the same pop and the same transitions
@Composable
private fun ReplyLike(liked: Boolean, count: Int, key: Int, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val scale = rememberLikePop(liked, key)
    val tint by animateColorAsState(
        if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "replyLikeTint"
    )

    Row(
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
    ) {
        Icon(
            imageVector = if (liked) KaisouIcons.HeartFilled else KaisouIcons.Heart,
            contentDescription = stringResource(R.string.story_like),
            tint = tint,
            modifier = Modifier
                .size(HomeDimens.ReplyActionIcon)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
        CountText(count, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// A count that slides up when it grows and down when it shrinks, and nothing when it is zero
@Composable
private fun CountText(count: Int, color: Color = KaisouTheme.colors.onMedia) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInVertically(tween(HomeDimens.CountMillis)) { direction * it } + fadeIn(tween(HomeDimens.CountMillis))) togetherWith
                (slideOutVertically(tween(HomeDimens.CountMillis)) { -direction * it } + fadeOut(tween(HomeDimens.CountMillis)))
        },
        label = "count"
    ) { value ->
        if (value > 0) {
            Text(text = value.toString(), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

// How many milliseconds a second has, for the moment an activity happened
private const val MillisPerSecond = 1000L

// Where an activity and a person are on AniList
private const val ActivityUrl = "https://anilist.co/activity/"
private const val UserUrl = "https://anilist.co/user/"

// A touch that lasts less than this is a tap, longer is holding
private const val HoldMillis = 250L
