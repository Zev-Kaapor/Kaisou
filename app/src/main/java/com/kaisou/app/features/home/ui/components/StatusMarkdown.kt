package com.kaisou.app.features.home.ui.components

// Compose text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

// The formats of the text of AniList that the editor offers, in the order of the two rows of buttons. The syntax is what goes into the
// text, with the cursor going to the offset inside it, and the ones that ask for an address have the place for it marked
enum class MarkdownFormat(val syntax: String, val offset: Int, val asksAddress: Boolean = false) {
    Bold("****", 2),
    Italic("**", 1),
    Strikethrough("~~~~", 2),
    Spoiler("~!!~", 2),
    Link("[link](%s)", 0, asksAddress = true),
    Image("img(%s)", 0, asksAddress = true),
    Youtube("youtube(%s)", 0, asksAddress = true),
    OrderedList("1. ", 3),
    UnorderedList("- ", 2),
    Heading("# ", 2),
    Center("~~~~~~", 3),
    Quote("> ", 2),
    Code("``", 1),
    Video("webm(%s)", 0, asksAddress = true)
}

// A text of AniList cut in the parts that are shown each in its way: the text, a picture, and a video that opens outside the app
sealed interface MarkdownBlock {
    data class Text(val text: String) : MarkdownBlock
    data class Image(val url: String) : MarkdownBlock
    data class Video(val url: String, val youtube: Boolean) : MarkdownBlock
}

// What the editor does with the text of AniList: puts a format in it, shows it styled while it is typed, and shows it as the site will
object StatusMarkdown {
    // A selection is wrapped in the syntax, or turned into a list line by line, and with nothing selected the syntax is put at the
    // cursor with the cursor inside it. The ones that ask for an address are given it
    fun apply(value: TextFieldValue, format: MarkdownFormat, address: String? = null): TextFieldValue {
        val text = value.text
        val start = value.selection.min
        val end = value.selection.max
        val selected = text.substring(start, end)

        val inserted: String
        val cursor: Int
        when {
            format.asksAddress -> {
                inserted = format.syntax.replace("%s", address.orEmpty())
                cursor = inserted.length
            }
            selected.isEmpty() -> {
                inserted = format.syntax
                cursor = format.offset
            }
            format == MarkdownFormat.UnorderedList -> {
                inserted = selected.lines().joinToString("\n") { "- $it" }
                cursor = inserted.length
            }
            format == MarkdownFormat.OrderedList -> {
                inserted = selected.lines().mapIndexed { index, line -> "${index + 1}. $line" }.joinToString("\n")
                cursor = inserted.length
            }
            else -> {
                inserted = format.syntax.substring(0, format.offset) + selected + format.syntax.substring(format.offset)
                cursor = inserted.length
            }
        }
        return TextFieldValue(text.replaceRange(start, end, inserted), TextRange(start + cursor))
    }

    // The same text with the markers faint and what is between them styled. Nothing is taken out, so the cursor stays where it is
    fun highlight(text: String, marker: Color, quote: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        for (inline in Inlines) {
            inline.regex.findAll(text).forEach { match ->
                val inner = match.groups[1] ?: return@forEach
                if (inner.range.isEmpty()) return@forEach
                addStyle(SpanStyle(color = marker), match.range.first, inner.range.first)
                addStyle(SpanStyle(color = marker), inner.range.last + 1, match.range.last + 1)
                inline.style?.let { addStyle(it, inner.range.first, inner.range.last + 1) }
            }
        }
        QuoteLine.findAll(text).forEach { addStyle(SpanStyle(color = quote, fontStyle = FontStyle.Italic), it.range.first, it.range.last + 1) }
        HeadingLine.findAll(text).forEach { addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.range.first, it.range.last + 1) }
    }

    // The text cut where a picture or a video is, each one a part of its own. The text between them keeps its lines
    fun blocks(text: String): List<MarkdownBlock> {
        val result = mutableListOf<MarkdownBlock>()
        var cursor = 0
        MediaMarkup.findAll(text).forEach { match ->
            text.substring(cursor, match.range.first).trim().takeIf { it.isNotEmpty() }?.let { result += MarkdownBlock.Text(it) }
            val kind = match.groupValues[1].lowercase()
            val url = match.groupValues[2].trim()
            result += if (kind == "img") MarkdownBlock.Image(url) else MarkdownBlock.Video(url, youtube = kind == "youtube")
            cursor = match.range.last + 1
        }
        text.substring(cursor).trim().takeIf { it.isNotEmpty() }?.let { result += MarkdownBlock.Text(it) }
        return result
    }

    // A part of text as the site shows it: the markers are gone and what they asked for is applied, the links open when touched, and only
    // what is between ~~~ and ~~~ is centered, over one line or several. The rest stays at the start, as in the site
    fun render(text: String, link: Color, heading: TextUnit): AnnotatedString = buildAnnotatedString {
        var cursor = 0
        CenterBlock.findAll(text).forEach { match ->
            lines(text.substring(cursor, match.range.first).trimEnd('\n'), link, heading)
            // A paragraph already starts on a line of its own, so the line breaks around it are not added again
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) { lines(match.groupValues[1].trim('\n'), link, heading) }
            cursor = match.range.last + 1
            while (cursor < text.length && text[cursor] == '\n') cursor++
        }
        lines(text.substring(cursor), link, heading)
    }

    // The lines of a text, each one as what it is: a quote, a title, an item of a list or a line of text
    private fun AnnotatedString.Builder.lines(text: String, link: Color, heading: TextUnit) {
        text.lines().forEachIndexed { index, line ->
            if (index > 0) append("\n")
            when {
                line.startsWith("> ") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { inline(line.removePrefix("> "), link) }
                HeadingLine.matches(line) -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = heading)) {
                    inline(line.replace(HeadingMark, ""), link)
                }
                line.startsWith("- ") -> {
                    append("\u2022 ")
                    inline(line.removePrefix("- "), link)
                }
                else -> inline(line, link)
            }
        }
    }

    // A line with its markers resolved, the nearest one first and what is inside it resolved the same way
    private fun AnnotatedString.Builder.inline(text: String, link: Color) {
        val linkStyle = TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline))
        var cursor = 0
        while (cursor < text.length) {
            val next = Inlines.mapNotNull { inline -> inline.regex.find(text, cursor)?.let { inline to it } }.minByOrNull { it.second.range.first }
            if (next == null) {
                append(text.substring(cursor))
                break
            }
            val (inline, match) = next
            append(text.substring(cursor, match.range.first))
            val inner = match.groupValues[1]
            when (inline.kind) {
                Kind.Emphasis -> withStyle(inline.style ?: SpanStyle()) { inline(inner, link) }
                Kind.Link -> withLink(LinkAnnotation.Url(match.groupValues[2].trim(), linkStyle)) { append(inner) }
                Kind.Url -> withLink(LinkAnnotation.Url(inner, linkStyle)) { append(inner) }
                Kind.Media -> Unit
            }
            cursor = match.range.last + 1
        }
    }

    private enum class Kind { Emphasis, Link, Url, Media }

    // A marker of the text, with what is inside it in the first group
    private class Inline(val regex: Regex, val kind: Kind, val style: SpanStyle?)

    private val Inlines = listOf(
        Inline(Regex("~~~([^~]+)~~~"), Kind.Emphasis, null),
        Inline(Regex("~!([^!]+)!~"), Kind.Emphasis, SpanStyle(background = Color(0x40FFFFFF))),
        Inline(Regex("~~([^~\\n]+)~~"), Kind.Emphasis, SpanStyle(textDecoration = TextDecoration.LineThrough)),
        Inline(Regex("\\*\\*([^*\\n]+)\\*\\*"), Kind.Emphasis, SpanStyle(fontWeight = FontWeight.Bold)),
        Inline(Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)"), Kind.Emphasis, SpanStyle(fontStyle = FontStyle.Italic)),
        Inline(Regex("`([^`\\n]+)`"), Kind.Emphasis, SpanStyle(background = Color(0x30FFFFFF), fontFamily = FontFamily.Monospace)),
        Inline(Regex("\\[([^\\]\\n]+)]\\(([^)\\n]*)\\)"), Kind.Link, null),
        Inline(Regex("(?i)(img|webm|youtube)\\d*\\(([^)\\n]*)\\)"), Kind.Media, null),
        Inline(Regex("(https?://[^\\s)\\]]+)"), Kind.Url, null)
    )
    private val MediaMarkup = Regex("(?i)(img|webm|youtube)\\d*\\(([^)\\n]*)\\)")
    private val QuoteLine = Regex("(?m)^> .*$")
    private val HeadingLine = Regex("(?m)^#{1,3} .*$")
    private val HeadingMark = Regex("^#{1,3} ")
    private val CenterBlock = Regex("~~~([\\s\\S]+?)~~~")
}
