package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

// Width of the bar, the shortest its thumb gets, and how strong the track and the thumb are
val ScrollIndicatorWidth = 4.dp
private val ThumbMinHeight = 32.dp

// How strong the track and the thumb are
private const val TrackAlpha = 0.08f
private const val ThumbAlpha = 0.6f

// How much of the content is on the screen and how far it was scrolled, both from 0 to 1
private class ScrollShare(val visible: Float, val scrolled: Float)

// A thin bar at the end of what scrolls, a faint track with a violet thumb, drawn over the part on the screen
@Composable
fun Modifier.scrollIndicator(state: ScrollState): Modifier = indicator { viewport ->
    if (state.maxValue <= 0 || state.maxValue == Int.MAX_VALUE) null
    else ScrollShare(viewport / (viewport + state.maxValue), state.value.toFloat() / state.maxValue)
}

// The same bar for a lazy list, whose whole length is guessed from the rows on the screen
@Composable
fun Modifier.scrollIndicator(state: LazyListState): Modifier = indicator {
    val info = state.layoutInfo
    val rows = info.visibleItemsInfo
    if (rows.isEmpty() || !(state.canScrollForward || state.canScrollBackward)) return@indicator null

    val span = rows.sumOf { it.size }.toFloat() / rows.size + info.mainAxisItemSpacing
    val viewport = info.viewportSize.height.toFloat()
    val maxScroll = (span * info.totalItemsCount - info.mainAxisItemSpacing - viewport).coerceAtLeast(1f)
    val scrolled = span * state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset
    ScrollShare(viewport / (viewport + maxScroll), (scrolled / maxScroll).coerceIn(0f, 1f))
}

// The same bar for a grid, whose whole length is guessed from the rows on the screen and how many items go in each of them
@Composable
fun Modifier.scrollIndicator(state: LazyGridState): Modifier = indicator {
    val info = state.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty() || !(state.canScrollForward || state.canScrollBackward)) return@indicator null

    val rows = items.groupBy { it.row }
    val perRow = (items.size.toFloat() / rows.size).coerceAtLeast(1f)
    val span = rows.values.sumOf { line -> line.maxOf { it.size.height } }.toFloat() / rows.size + info.mainAxisItemSpacing
    val viewport = info.viewportSize.height.toFloat()
    val totalRows = info.totalItemsCount / perRow
    val maxScroll = (span * totalRows - info.mainAxisItemSpacing - viewport).coerceAtLeast(1f)
    val scrolled = span * (state.firstVisibleItemIndex / perRow) + state.firstVisibleItemScrollOffset
    ScrollShare(viewport / (viewport + maxScroll), (scrolled / maxScroll).coerceIn(0f, 1f))
}

// Draws the track and the thumb, the share is read while drawing so scrolling only draws again
@Composable
private fun Modifier.indicator(share: (viewport: Float) -> ScrollShare?): Modifier {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TrackAlpha)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = ThumbAlpha)

    return drawWithContent {
        drawContent()
        val current = share(size.height) ?: return@drawWithContent

        val width = ScrollIndicatorWidth.toPx()
        val corner = CornerRadius(width / 2)
        val left = size.width - width
        val thumbHeight = (size.height * current.visible).coerceAtLeast(ThumbMinHeight.toPx())
        val thumbTop = (size.height - thumbHeight) * current.scrolled

        drawRoundRect(color = trackColor, topLeft = Offset(left, 0f), size = Size(width, size.height), cornerRadius = corner)
        drawRoundRect(color = thumbColor, topLeft = Offset(left, thumbTop), size = Size(width, thumbHeight), cornerRadius = corner)
    }
}

// Where a list was left, kept by the view model so it survives turning the phone, since each layout has its own
class ScrollPosition {
    var index = 0
    var offset = 0
}
