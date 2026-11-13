package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
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

// Pill with a centered label and an optional icon at the start, the caller gives the background
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Optional icon at the start, the label stays centered
        if (leading != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = dimens.spaceSmall)
            ) {
                leading()
            }
        }

        // Room on both sides for the area of the icon, so a long label never runs under it
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = ComponentDimens.ButtonLeading + dimens.spaceMedium)
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
