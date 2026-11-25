package com.kaisou.app.features.title.data

// A part of a text of the forum: the text, or a link to a title that stands alone in its line, which the site shows as a card
sealed interface ForumPart {
    // A piece of text of a forum post
    data class Text(val text: String) : ForumPart

    // A card of a title linked in a forum post
    data class Card(val id: Int) : ForumPart
}

// A title that a text of the forum links to, to show as a card: its name, its cover and a line about it
data class LinkedTitle(val id: Int, val title: String, val cover: String?, val info: String)

// Finds the links to a title of AniList that stand alone in a line of a text, as the site turns them into a card
object ForumLinks {
    private val link = Regex("""^\s*https?://anilist\.co/(?:anime|manga)/(\d+)\S*\s*$""", RegexOption.IGNORE_CASE)

    // The ids of the titles that the lines of the text link to, in the order they come and with no repeat
    fun ids(text: String): List<Int> = text.lines().mapNotNull { link.find(it)?.groupValues?.get(1)?.toIntOrNull() }.distinct()

    // The text cut where a link of a known title is, a link to an unknown title staying in the text
    fun split(text: String, known: Set<Int>): List<ForumPart> {
        val parts = mutableListOf<ForumPart>()
        val pending = mutableListOf<String>()

        // Closes the text gathered so far as a piece
        fun flush() {
            pending.joinToString("\n").trim('\n').takeIf { it.isNotEmpty() }?.let { parts += ForumPart.Text(it) }
            pending.clear()
        }

        for (line in text.lines()) {
            val id = link.find(line)?.groupValues?.get(1)?.toIntOrNull()
            if (id != null && id in known) {
                flush()
                parts += ForumPart.Card(id)
            } else {
                pending += line
            }
        }
        flush()
        return parts
    }
}
