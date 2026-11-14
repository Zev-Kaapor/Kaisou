package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.components.ConfirmDialog
import com.kaisou.app.core.designsystem.components.ImmersiveDialogEffect
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.ComposerUi
import com.kaisou.app.features.home.ui.HomeDimens

// The screen to write a status, over the Home. The bar on top has the way back, the preview of how the text will look and the button
// that posts it, which first asks if the user is sure, since it goes to their account. The text shows its markers faint and what they
// style already styled while it is typed, and the buttons of the formats stay over the keyboard. When AniList takes the status the
// screen closes
@Composable
fun StatusEditor(composer: ComposerUi, onPost: (String) -> Unit, onClose: () -> Unit) {
    // Whether there is a text that would be lost, told by the screen, and whether the user is being asked about leaving
    var hasText by remember { mutableStateOf(false) }
    var discarding by rememberSaveable { mutableStateOf(false) }
    // The way back, the back button and a touch outside leave at once when there is nothing written, and ask first when there is
    val leave = { if (hasText && !composer.sent) discarding = true else onClose() }

    Dialog(
        onDismissRequest = leave,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ImmersiveDialogEffect()
        StatusEditorContent(composer = composer, onPost = onPost, onBack = leave, onClose = onClose, onHasText = { hasText = it })
    }

    if (discarding) {
        ConfirmDialog(
            title = stringResource(R.string.status_discard_title),
            text = stringResource(R.string.status_discard_text),
            confirmLabel = stringResource(R.string.status_discard_confirm),
            cancelLabel = stringResource(R.string.status_discard_cancel),
            onConfirm = {
                discarding = false
                onClose()
            },
            onDismiss = { discarding = false }
        )
    }
}

@Composable
private fun StatusEditorContent(
    composer: ComposerUi,
    onPost: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onHasText: (Boolean) -> Unit
) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var asking by remember { mutableStateOf<MarkdownFormat?>(null) }
    val focus = remember { FocusRequester() }
    val canPost = value.text.isNotBlank() && !composer.sending
    // Lying down there is little height, so the buttons of the formats go in a single row
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val context = LocalContext.current

    LaunchedEffect(composer.sent) { if (composer.sent) onClose() }
    LaunchedEffect(value.text.isNotBlank()) { onHasText(value.text.isNotBlank()) }
    LaunchedEffect(preview) { if (!preview) focus.requestFocus() }

    val faint = MaterialTheme.colorScheme.onSurfaceVariant
    val highlighter = remember(faint) {
        VisualTransformation { text ->
            TransformedText(StatusMarkdown.highlight(text.text, marker = faint.copy(alpha = HomeDimens.MarkerAlpha), quote = faint), OffsetMapping.Identity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        // The way back, the preview and the post
        Row(
            modifier = Modifier.fillMaxWidth().height(dimens.buttonHeight).padding(start = dimens.spaceSmall, end = dimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = KaisouIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimens.buttonHeight).clip(CircleShape).clickable(onClick = onBack).padding(dimens.spaceMedium)
            )
            Text(
                text = stringResource(R.string.status_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier.clip(CircleShape).clickable { preview = !preview }.padding(end = dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = preview, onCheckedChange = { preview = it })
                Text(text = stringResource(R.string.status_preview), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            Box(
                modifier = Modifier
                    .size(HomeDimens.StatusSendSize)
                    .alpha(if (canPost || composer.sending) 1f else HomeDimens.StatusDisabledAlpha)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.buttonStart, colors.buttonEnd)))
                    .clickable(enabled = canPost) { confirming = true },
                contentAlignment = Alignment.Center
            ) {
                if (composer.sending) {
                    CircularProgressIndicator(
                        color = colors.onAccentFill,
                        strokeWidth = HomeDimens.StatusSpinnerStroke,
                        modifier = Modifier.size(HomeDimens.StatusSpinner)
                    )
                } else {
                    Icon(imageVector = KaisouIcons.Send, contentDescription = stringResource(R.string.status_post), tint = colors.onAccentFill, modifier = Modifier.size(HomeDimens.StatusSendIcon))
                }
            }
        }

        // The text, with its markers styled while it is typed, or as the site will show it
        Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(dimens.screenPadding)) {
            if (preview) {
                MarkdownContent(
                    text = value.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (value.text.isEmpty()) {
                    Text(text = stringResource(R.string.status_hint), style = MaterialTheme.typography.bodyLarge, color = faint)
                }
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = highlighter,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    enabled = !composer.sending,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
        }

        if (composer.failed) {
            Text(
                text = stringResource(R.string.status_failed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
            )
        }

        // The buttons of the formats, two rows of seven, over the keyboard. They do nothing while the preview is on
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall).alpha(if (preview) HomeDimens.StatusDisabledAlpha else 1f)
        ) {
            MarkdownFormat.entries.chunked(if (landscape) MarkdownFormat.entries.size else FormatsPerRow).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { format ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(dimens.buttonHeight)
                                .clip(CircleShape)
                                .clickable(enabled = !preview) {
                                    if (format.asksAddress) asking = format else value = StatusMarkdown.apply(value, format)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = format.icon(),
                                contentDescription = stringResource(format.description()),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = stringResource(R.string.status_confirm_title),
            text = stringResource(R.string.status_confirm_text),
            confirmLabel = stringResource(R.string.status_post),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                confirming = false
                onPost(value.text)
            },
            onDismiss = { confirming = false },
            linkLabel = stringResource(R.string.status_learn_more),
            onLink = { InAppBrowser.open(context, RulesUrl) }
        )
    }
    asking?.let { format ->
        AddressDialog(
            onConfirm = { address ->
                value = StatusMarkdown.apply(value, format, address)
                asking = null
            },
            onDismiss = { asking = null }
        )
    }
}

// Asks for the address of a link, a picture or a video, to put it in the text
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val dimens = KaisouTheme.dimens
    var address by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.padding(dimens.spaceExtraLarge), verticalArrangement = Arrangement.spacedBy(dimens.spaceLarge)) {
                Text(text = stringResource(R.string.status_address_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.status_address_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.action_cancel), maxLines = 1)
                    }
                    Button(
                        onClick = { onConfirm(address.trim()) },
                        enabled = address.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.status_address_add), maxLines = 1)
                    }
                }
            }
        }
    }
}

// The icon and the name of each format
private fun MarkdownFormat.icon(): ImageVector = when (this) {
    MarkdownFormat.Bold -> KaisouIcons.Bold
    MarkdownFormat.Italic -> KaisouIcons.Italic
    MarkdownFormat.Strikethrough -> KaisouIcons.Strikethrough
    MarkdownFormat.Spoiler -> KaisouIcons.Spoiler
    MarkdownFormat.Link -> KaisouIcons.Link
    MarkdownFormat.Image -> KaisouIcons.Image
    MarkdownFormat.Youtube -> KaisouIcons.Youtube
    MarkdownFormat.OrderedList -> KaisouIcons.ListOrdered
    MarkdownFormat.UnorderedList -> KaisouIcons.ListBulleted
    MarkdownFormat.Heading -> KaisouIcons.Heading
    MarkdownFormat.Center -> KaisouIcons.AlignCenter
    MarkdownFormat.Quote -> KaisouIcons.Quote
    MarkdownFormat.Code -> KaisouIcons.Code
    MarkdownFormat.Video -> KaisouIcons.Video
}

private fun MarkdownFormat.description(): Int = when (this) {
    MarkdownFormat.Bold -> R.string.md_bold
    MarkdownFormat.Italic -> R.string.md_italic
    MarkdownFormat.Strikethrough -> R.string.md_strikethrough
    MarkdownFormat.Spoiler -> R.string.md_spoiler
    MarkdownFormat.Link -> R.string.md_link
    MarkdownFormat.Image -> R.string.md_image
    MarkdownFormat.Youtube -> R.string.md_youtube
    MarkdownFormat.OrderedList -> R.string.md_ordered
    MarkdownFormat.UnorderedList -> R.string.md_bulleted
    MarkdownFormat.Heading -> R.string.md_heading
    MarkdownFormat.Center -> R.string.md_center
    MarkdownFormat.Quote -> R.string.md_quote
    MarkdownFormat.Code -> R.string.md_code
    MarkdownFormat.Video -> R.string.md_video
}

// How many buttons of formats are in a row, and where the rules of AniList for what is posted are
private const val FormatsPerRow = 7
private const val RulesUrl = "https://anilist.co/forum/thread/14"
