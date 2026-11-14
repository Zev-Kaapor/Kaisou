package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// Main pill button filled with the violet of the brand, with something at its start: an icon, a badge or a spinner
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    val colors = KaisouTheme.colors

    PillButton(
        text = text,
        textColor = colors.onAccentFill,
        onClick = onClick,
        modifier = modifier.background(Brush.horizontalGradient(listOf(colors.buttonStart, colors.buttonEnd)), CircleShape),
        leading = if (loading) {
            { Spinner(colors.onAccentFill) }
        } else {
            leading
        },
        enabled = enabled
    )
}

// Pill button on a quiet surface, for what comes second
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    PillButton(
        text = text,
        textColor = MaterialTheme.colorScheme.onBackground,
        onClick = onClick,
        modifier = modifier.background(KaisouTheme.colors.elevated, CircleShape),
        leading = leading,
        enabled = enabled
    )
}

// A plain icon for the start of a pill button, in the same area a badge or a spinner takes
@Composable
fun ButtonIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(ComponentDimens.ButtonLeading),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(ComponentDimens.ButtonIcon)
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
            .size(ComponentDimens.IconButton)
            .border(ComponentDimens.ButtonBorder, accent, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier.size(ComponentDimens.IconButtonIcon)
        )
    }
}

// Pill with the label and an optional icon at its start, together in the middle. The caller gives the background
@Composable
private fun PillButton(
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = dimens.spaceLarge),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(dimens.spaceSmall))
        }
        // A long label gives way before it runs under the icon or out of the button
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// Spinner for the start of a button while it is busy
@Composable
private fun Spinner(color: Color) {
    Box(
        modifier = Modifier.size(ComponentDimens.ButtonLeading),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(ComponentDimens.ButtonIcon),
            color = color,
            strokeWidth = ComponentDimens.ButtonSpinnerStroke
        )
    }
}
