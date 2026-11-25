package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Width of the light band as a share of the host width, and how much it leans
private const val BandWidthFraction = 0.6f
private const val BandTilt = 0.4f

// Nanoseconds in a millisecond
private const val NanosPerMilli = 1_000_000L

// Flat placeholder shape, the light comes from the SkeletonHost around it
@Composable
fun Modifier.skeleton(shape: Shape = MaterialTheme.shapes.extraSmall): Modifier =
    clip(shape).background(KaisouTheme.colors.skeleton)

// How far the light is in its way across, from 0 to 1, and 0 while it waits for the next one
@Composable
private fun rememberSweepProgress(
    sweepMillis: Int = KaisouMotion.SkeletonSweepMillis,
    pauseMillis: Int = KaisouMotion.SkeletonPauseMillis
): State<Float> {
    val progress = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(sweepMillis, pauseMillis) {
        val period = (sweepMillis + pauseMillis).toLong()
        while (true) {
            withFrameNanos { nanos ->
                val time = (nanos / NanosPerMilli) % period
                progress.floatValue = (time - pauseMillis).coerceAtLeast(0L).toFloat() / sweepMillis
            }
        }
    }
    return progress
}

// A light that passes over the content now and then to call the eye, lighting only what is drawn
@Composable
fun Modifier.shine(enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val highlight = MaterialTheme.colorScheme.onBackground
    val clear = highlight.copy(alpha = 0f)
    val progress by rememberSweepProgress(KaisouMotion.ShineSweepMillis, KaisouMotion.ShinePauseMillis)

    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val band = size.width * BandWidthFraction
        val start = -band + (size.width + band) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(clear, highlight, clear),
                start = Offset(start, 0f),
                end = Offset(start + band, band * BandTilt)
            ),
            blendMode = BlendMode.SrcAtop
        )
    }
}

// Holds the placeholders of a part of a screen, one light sweeps over all of them, every block whole
@Composable
fun SkeletonHost(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val highlight = KaisouTheme.colors.skeletonHighlight
    val clear = highlight.copy(alpha = 0f)

    // The light follows the clock of the frames, so it keeps its pace and every placeholder has it in the same place
    val progress by rememberSweepProgress()

    Column(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()

                // Light only over the shapes, from before the left edge to past the right edge
                val band = size.width * BandWidthFraction
                val start = -band + (size.width + band) * progress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(clear, highlight, clear),
                        start = Offset(start, 0f),
                        end = Offset(start + band, band * BandTilt)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            },
        content = content
    )
}

// Lines of text as placeholders, the last one shorter, to go inside a SkeletonHost
@Composable
fun SkeletonLines(
    lines: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ComponentDimens.SkeletonLineGap)
    ) {
        repeat(lines) { index ->
            val last = index == lines - 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (last) ComponentDimens.SkeletonLastLineFraction else 1f)
                    .height(ComponentDimens.SkeletonLineHeight)
                    .skeleton()
            )
        }
    }
}
