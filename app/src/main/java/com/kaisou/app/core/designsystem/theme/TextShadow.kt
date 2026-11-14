package com.kaisou.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity

// Soft dark shadow around the letters, for the texts over pictures
@Composable
fun textShadow(): Shadow {
    val density = LocalDensity.current
    val dimens = KaisouTheme.dimens
    val color = KaisouTheme.colors.textShadow

    return with(density) {
        Shadow(
            color = color,
            offset = Offset(0f, dimens.textShadowOffset.toPx()),
            blurRadius = dimens.textShadowBlur.toPx()
        )
    }
}
