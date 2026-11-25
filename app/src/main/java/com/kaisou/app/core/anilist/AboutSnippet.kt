package com.kaisou.app.core.anilist

// Project resources
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.translation.GoogleTranslator

// The beginning of what a character or a person of the staff says of itself, in a line or two, for the cards of the lists
object AboutSnippet {
    // How long it is, what a line of data it opens with looks like and what a link looks like
    private const val Length = 140
    private val DataLead = Regex("^(?:~!)?\\s*(?:__|\\*\\*)\\s*[^_*\\n]{1,40}?:\\s*(?:__|\\*\\*)|^(?:~!)?\\s*(?:__|\\*\\*)\\s*[^_*\\n]{1,40}?\\s*(?:__|\\*\\*)\\s*:")
    private val MarkdownLink = Regex("\\[([^\\]]+)]\\([^)]*\\)")

    // Cuts the lines of data, turns links into words and closes a cut cover so a spoiler never shows
    fun of(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var text = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.dropWhile { DataLead.containsMatchIn(it) }.joinToString(" ")

        // A cover that opened on the lines of data and closed after them leaves its end at the start
        val close = text.indexOf("!~")
        if (close >= 0 && (text.indexOf("~!").let { it < 0 || it > close })) text = text.substring(close + 2).trim()
        text = MarkdownLink.replace(text, "$1")
        if (text.isBlank()) return null
        if (text.length > Length) {
            val cut = text.lastIndexOf(' ', Length).takeIf { it > 0 } ?: Length
            text = text.substring(0, cut).trimEnd()
            if (text.windowed(2).count { it == "~!" } > text.windowed(2).count { it == "!~" }) text += "!~"
            text += "…"
        }
        return text
    }

    // Puts some beginnings in the language of the app at once, unchanged when it is AniList's or fails
    suspend fun translated(texts: List<String>): List<String> =
        if (!AppLanguage.needsTranslation || texts.isEmpty()) texts else GoogleTranslator.translateLines(texts, AppLanguage.translationCode).map(AniDescription::tidy)
}
