package com.kaisou.app.features.home.ui.components

// Android framework
import android.content.Context
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.navigation.LocalAniLinks
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens

// A text of AniList on a screen: the text with its markers resolved and its links that open when touched, a picture as a picture
// that opens at full size outside the app when touched, and a video as a card that opens it outside the app
@Composable
fun MarkdownContent(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, textAlign: TextAlign? = null, hideSpoilers: Boolean = false) {
    val dimens = KaisouTheme.dimens
    val link = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val aniLinks = LocalAniLinks.current
    val uriHandler = remember(context, aniLinks) { InAppUriHandler(context, aniLinks) }
    val blocks = remember(text) { StatusMarkdown.blocks(text) }

    // The links of the text, and the pictures and the videos, open in the browser that stays over the app
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> if (hideSpoilers) {
                    SpoilerText(block.text, style, color, link, HomeDimens.StatusHeadingSize, textAlign = textAlign)
                } else {
                    val rendered = remember(block.text, link) { StatusMarkdown.render(block.text, link, HomeDimens.StatusHeadingSize) }
                    Text(text = rendered, style = style, color = color, textAlign = textAlign, modifier = Modifier.fillMaxWidth())
                }
                // A picture has the width it was asked: pixels, as far as the line allows, or a share of the line. One that was not asked
                // any takes the line, and stands at the start of it unless it was in a centered part
                is MarkdownBlock.Image -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (block.centered) Alignment.TopCenter else Alignment.TopStart) {
                    val sized = block.width != null
                    AsyncImage(
                        model = block.url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .then(
                                when {
                                    block.width == null -> Modifier.fillMaxWidth()
                                    block.percent -> Modifier.fillMaxWidth(block.width.coerceAtMost(PercentWhole) / PercentWhole.toFloat())
                                    else -> Modifier.widthIn(max = block.width.dp).fillMaxWidth()
                                }
                            )
                            .then(if (sized) Modifier else Modifier.heightIn(max = HomeDimens.StatusImageMaxHeight))
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(role = Role.Button) { uriHandler.openUri(block.url) }
                    )
                }
                is MarkdownBlock.Video -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(KaisouTheme.colors.elevated)
                        .clickable(role = Role.Button) { uriHandler.openUri(block.url) }
                        .padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
                ) {
                    Icon(
                        imageVector = KaisouIcons.PlayOutline,
                        contentDescription = null,
                        tint = link,
                        modifier = Modifier.size(HomeDimens.StatusVideoIcon)
                    )
                    Text(
                        text = stringResource(if (block.youtube) R.string.md_open_youtube else R.string.md_open_video),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    }
}

// The whole of a line in percent, which is the most a picture can ask for
private const val PercentWhole = 100

// Opens the addresses of the links of a text in the browser that stays over the app
private class InAppUriHandler(private val context: Context, private val inApp: (String) -> Boolean) : UriHandler {
    // The addresses of a title, a character, a person of the staff or a studio open their own pages, the others open in the browser
    override fun openUri(uri: String) {
        if (!inApp(uri)) InAppBrowser.open(context, uri)
    }
}
