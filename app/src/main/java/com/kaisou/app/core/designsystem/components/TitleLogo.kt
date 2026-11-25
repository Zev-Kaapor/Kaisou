package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

// Name of a title: its logo as big as fits keeping its shape, or the name in text sized by its length and the screen
@Composable
fun TitleLogo(
    title: String,
    logo: ImageRequest?,
    modifier: Modifier = Modifier,
    maxHeight: Dp = ComponentDimens.TitleLogoMaxHeight
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier.sizeIn(
                    maxWidth = maxWidth * ComponentDimens.TitleLogoWidthFraction,
                    maxHeight = maxHeight
                )
            )
            return@BoxWithConstraints
        }

        val step = (title.length / ComponentDimens.TitleCharsPerStep)
            .coerceIn(0, ComponentDimens.TitleSizes.lastIndex)
        val widthScale = (1f + (maxWidth.value / ComponentDimens.TitleReferenceWidth - 1f) * ComponentDimens.TitleScaleWeight)
            .coerceIn(ComponentDimens.TitleScaleMin, ComponentDimens.TitleScaleMax)
        val size = ComponentDimens.TitleSizes[step].value * widthScale

        BasicText(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = size.sp,
                lineHeight = (size * ComponentDimens.TitleLineHeightRatio).sp
            ),
            maxLines = ComponentDimens.TitleMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
