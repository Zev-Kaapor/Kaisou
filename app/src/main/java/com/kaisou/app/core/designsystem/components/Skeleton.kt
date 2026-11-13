package com.kaisou.app.core.designsystem.components

// Compose animation
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Width of the light band as a share of the host width, and how much it leans
private const val BandWidthFraction = 0.6f
private const val BandTilt = 0.4f

// Flat placeholder shape, the light comes from the SkeletonHost around it
@Composable
fun Modifier.skeleton(shape: Shape = MaterialTheme.shapes.extraSmall): Modifier =
    clip(shape).background(KaisouTheme.colors.skeleton)

// Holds the placeholders of a part of a screen, one light sweeps over all of them, every block whole
@Composable
fun SkeletonHost(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val highlight = KaisouTheme.colors.skeletonHighlight
    val clear = highlight.copy(alpha = 0f)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = ComponentDimens.SkeletonSweepMillis,
                delayMillis = ComponentDimens.SkeletonPauseMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonSweep"
    )

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
