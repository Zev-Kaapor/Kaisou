package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Pictures
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// A picture of the viewer: where the whole one is, a small one that shows while it comes and the words that name it on top
class ViewerImage(val url: String, val placeholder: String? = null, val title: String? = null)

// The pictures over the whole screen on black, in the size they have, with no cut by the size of the screen. One slides to the next with a swipe,
// and the one that is on shows its name and its place on top. It is zoomed with two fingers or with two touches, both with an animation, and
// moved when it is zoomed. A touch hides or shows the buttons: the first closes and the other, when the pictures can be downloaded, saves the one
// that is on. What the download says is shown over the picture, for a moment
@Composable
fun ImageViewer(
    images: List<ViewerImage>,
    onDismiss: () -> Unit,
    startIndex: Int = 0,
    onDownload: ((ViewerImage) -> String)? = null
) {
    val dimens = KaisouTheme.dimens
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))) { images.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val current = images.getOrNull(pager.currentPage)

    // The note of the download goes away by itself
    LaunchedEffect(note) {
        if (note != null) {
            delay(NoteMillis)
            note = null
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize()) { page ->
                ViewerPage(
                    image = images[page],
                    active = page == pager.currentPage,
                    onZoomed = { zoomed = it },
                    onTap = { chrome = !chrome }
                )
            }

            if (chrome) {
                Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(dimens.spaceSmall)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                        ViewerButton(icon = KaisouIcons.Close, description = stringResource(R.string.cd_close), onClick = onDismiss)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            current?.title?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (images.size > 1) {
                                Text(
                                    text = "${pager.currentPage + 1} / ${images.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (onDownload != null && current != null) {
                            ViewerButton(icon = KaisouIcons.Download, description = stringResource(R.string.cd_download), onClick = { note = onDownload(current) })
                        } else {
                            Box(modifier = Modifier.size(ComponentDimens.IconButton))
                        }
                    }
                }
            }
            note?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(dimens.screenPadding)
                        .clip(CircleShape)
                        .background(KaisouTheme.colors.elevated)
                        .padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceSmall)
                )
            }
        }
    }
}

// One picture of the viewer, which keeps its own zoom. The zoom and the move follow the fingers and the double touch and the end of a pinch that
// ended too small go to their place with an animation. A touch with one finger on a picture that is not zoomed is left for the viewer to slide
@Composable
private fun ViewerPage(image: ViewerImage, active: Boolean, onZoomed: (Boolean) -> Unit, onTap: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var running by remember { mutableStateOf<Job?>(null) }

    // How far a zoomed picture can be moved: what goes past the screen on each side
    fun limit(forScale: Float, value: Offset): Offset {
        val maxX = size.width * (forScale - 1f) / 2f
        val maxY = size.height * (forScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    // Goes to a zoom and a place with an animation
    fun animateTo(targetScale: Float, targetOffset: Offset) {
        running?.cancel()
        val fromScale = scale
        val fromOffset = offset
        running = scope.launch {
            Animatable(0f).animateTo(1f) {
                scale = fromScale + (targetScale - fromScale) * value
                offset = Offset(fromOffset.x + (targetOffset.x - fromOffset.x) * value, fromOffset.y + (targetOffset.y - fromOffset.y) * value)
            }
            onZoomed(scale > 1f)
        }
    }

    // A page that is not the one on screen goes back to its size
    LaunchedEffect(active) {
        if (!active) {
            running?.cancel()
            scale = 1f
            offset = Offset.Zero
            onZoomed(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            animateTo(1f, Offset.Zero)
                        } else {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            animateTo(DoubleTapZoom, limit(DoubleTapZoom, (center - tap) * (DoubleTapZoom - 1f)))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectZoomAndMove(
                    isZoomed = { scale > 1f },
                    // A pinch that ended smaller than the picture goes back to it
                    onEnd = { if (scale < 1f) animateTo(1f, Offset.Zero) else onZoomed(scale > 1f) }
                ) { centroid, pan, zoom ->
                    running?.cancel()
                    val next = (scale * zoom).coerceIn(MinZoom, MaxZoom)
                    val applied = next / scale
                    val center = Offset(size.width / 2f, size.height / 2f)
                    offset = limit(next, offset * applied + (centroid - center) * (1f - applied) + pan)
                    scale = next
                    onZoomed(scale > 1f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val layer = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        image.placeholder?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = layer) }
        // The whole size of the picture, so zooming shows detail and not a picture cut to the size of the screen
        AsyncImage(
            model = ImageRequest.Builder(context).data(image.url).size(Size.ORIGINAL).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = layer
        )
    }
}

// Follows two fingers, or one when the picture is zoomed, and tells how much they zoomed and moved and where the middle of them is. With one finger
// on a picture that is not zoomed it leaves the touch alone, so the viewer can slide to the other picture. When the fingers go up it says it ended
private suspend fun PointerInputScope.detectZoomAndMove(
    isZoomed: () -> Boolean,
    onEnd: () -> Unit,
    onChange: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val consumed = event.changes.any { it.isConsumed }
            if (!consumed && (event.changes.size > 1 || isZoomed())) {
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                if (zoom != 1f || pan != Offset.Zero) {
                    onChange(event.calculateCentroidSafe(), pan, zoom)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!consumed && event.changes.any { !it.changedToUp() && it.pressed })
        onEnd()
    }
}

// The middle of the fingers that are down
private fun PointerEvent.calculateCentroidSafe(): Offset {
    val down = changes.filter { it.pressed }
    if (down.isEmpty()) return Offset.Zero
    return Offset(down.map { it.position.x }.average().toFloat(), down.map { it.position.y }.average().toFloat())
}

// A round button over the picture, with the dark of the page behind it so it is seen over a light picture too
@Composable
private fun ViewerButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ComponentDimens.IconButton)
            .clip(CircleShape)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
    }
}

private const val MinZoom = 0.8f
private const val MaxZoom = 6f
private const val DoubleTapZoom = 2.5f
private const val NoteMillis = 2_000L
