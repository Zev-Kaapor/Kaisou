package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// A question with two buttons side by side, the one that goes on standing out, which can end its text with a link
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    textAlign: TextAlign = TextAlign.Center,
    linkLabel: String? = null,
    onLink: () -> Unit = {}
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(ConfirmCorner),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(ConfirmPadding)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                val accent = MaterialTheme.colorScheme.primary
                Text(
                    text = buildAnnotatedString {
                        append(text)

                        // The link goes on the same paragraph, right after the last word
                        if (linkLabel != null) {
                            append(" ")
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = LinkTag,
                                    styles = TextLinkStyles(SpanStyle(color = accent)),
                                    linkInteractionListener = { onLink() }
                                )
                            ) { append(linkLabel) }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                    modifier = Modifier.padding(top = ConfirmTextTop)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ConfirmButtonGap),
                    modifier = Modifier.fillMaxWidth().padding(top = ConfirmButtonsTop)
                ) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = cancelLabel, maxLines = 1)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = confirmLabel, maxLines = 1)
                    }
                }
            }
        }
    }
}

// Name of the link at the end of the text
private const val LinkTag = "link"

// The corners of the box, the room inside it and between its parts
private val ConfirmCorner = 28.dp
private val ConfirmPadding = 24.dp
private val ConfirmTextTop = 12.dp
private val ConfirmButtonsTop = 24.dp
private val ConfirmButtonGap = 12.dp
