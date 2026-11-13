package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Main pill button filled with the accent
@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = modifier
            .height(dimens.buttonHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.spaceExtraLarge),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onPrimary,
            modifier = Modifier.size(ComponentDimens.ButtonIcon)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = onPrimary
        )
    }
}

// Round button with an outline in the accent, next to the main button
@Composable
fun OutlineIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(ComponentDimens.IconButtonLarge)
            .border(ComponentDimens.ButtonBorder, accent, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent
        )
    }
}
