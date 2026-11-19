package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// A text of AniList in which what gives away the story, between ~! and !~, is covered: it shows as a block of its own color and shows its
// words when it is touched, and covers again when it is touched again. The rest of the text has its markers resolved as everywhere else
@Composable
fun SpoilerText(
    text: String,
    style: TextStyle,
    color: Color,
    linkColor: Color,
    headingSize: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    val cover = KaisouTheme.colors.skeleton
    val parts = remember(text) { split(text) }
    // The covers that were opened, by their place in the text, which stay when the phone is turned
    var opened by rememberSaveable { mutableStateOf(listOf<Int>()) }

    val rendered = remember(parts, opened, cover, linkColor) {
        buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                if (!part.spoiler) {
                    append(StatusMarkdown.render(part.text, linkColor, headingSize))
                } else {
                    val shown = index in opened
                    val link = LinkAnnotation.Clickable(tag = "spoiler-$index") { opened = if (shown) opened - index else opened + index }
                    withLink(link) {
                        // Covered, the words have the color of the block, so only the block shows
                        withStyle(if (shown) SpanStyle(background = cover) else SpanStyle(color = cover, background = cover)) { append(part.text) }
                    }
                }
            }
        }
    }
    Text(text = rendered, style = style, color = color, textAlign = textAlign, modifier = modifier.fillMaxWidth())
}

private class Part(val text: String, val spoiler: Boolean)

private val Spoiler = Regex("~!(.+?)!~", RegexOption.DOT_MATCHES_ALL)

// The text cut in the pieces that are open and the ones that are covered, in order
private fun split(text: String): List<Part> {
    val parts = mutableListOf<Part>()
    var from = 0
    Spoiler.findAll(text).forEach { match ->
        if (match.range.first > from) parts += Part(text.substring(from, match.range.first), spoiler = false)
        parts += Part(match.groupValues[1], spoiler = true)
        from = match.range.last + 1
    }
    if (from < text.length) parts += Part(text.substring(from), spoiler = false)
    return parts
}
