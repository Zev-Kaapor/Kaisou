package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Name of a title as its logo, or in plain text when it has none. While the artwork is still being looked up
// the space stays empty, so the text does not show up for a moment and then give way to the logo
@Composable
fun TitleLogo(
    title: String,
    logo: ImageBitmap?,
    loading: Boolean,
    boxHeight: Dp,
    modifier: Modifier = Modifier
) {
    // A logo gets a box of its own height, a name in text takes only the room of its lines
    BoxWithConstraints(
        modifier = modifier.heightIn(min = if (loading || logo != null) boxHeight else 0.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when {
            loading -> SkeletonHost(modifier = Modifier.fillMaxWidth(ComponentDimens.TitleSkeletonWidthFraction)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ComponentDimens.TitleSkeletonHeight)
                        .skeleton()
                )
            }
            logo == null -> TitleText(title = title, contentWidth = maxWidth)
            else -> {
                val size = logoSize(
                    aspect = logo.width.toFloat() / logo.height,
                    available = maxWidth,
                    boxHeight = boxHeight
                )
                Image(
                    bitmap = logo,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

// Size of a logo already cut to what is drawn: square ones go by the height, wide ones by the width, none gets too small
private fun logoSize(aspect: Float, available: Dp, boxHeight: Dp): DpSize {
    var height = boxHeight
    var width = height * aspect
    val maxWidth = available * ComponentDimens.LogoMaxWidthFraction
    if (width > maxWidth) {
        width = maxWidth
        height = width / aspect
    }
    if (height < ComponentDimens.LogoMinHeight) {
        height = ComponentDimens.LogoMinHeight
        width = minOf(height * aspect, available)
        height = width / aspect
    }
    return DpSize(width, height)
}

// Name in text with the size picked by the length of the name and the width of the screen, up to a few lines
@Composable
private fun TitleText(title: String, contentWidth: Dp) {
    val step = (title.length / ComponentDimens.TitleCharsPerStep)
        .coerceIn(0, ComponentDimens.TitleSizes.lastIndex)
    val widthScale = (1f + (contentWidth.value / ComponentDimens.TitleReferenceWidth - 1f) * ComponentDimens.TitleScaleWeight)
        .coerceIn(ComponentDimens.TitleScaleMin, ComponentDimens.TitleScaleMax)
    val size = ComponentDimens.TitleSizes[step].value * widthScale

    BasicText(
        text = title,
        style = MaterialTheme.typography.displayLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = size.sp,
            lineHeight = (size * ComponentDimens.TitleLineHeightRatio).sp
        ),
        maxLines = ComponentDimens.TitleMaxLines,
        overflow = TextOverflow.Ellipsis
    )
}
