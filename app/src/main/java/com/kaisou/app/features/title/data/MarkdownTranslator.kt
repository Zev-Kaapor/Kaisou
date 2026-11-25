package com.kaisou.app.features.title.data

// Coroutines
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
// Project resources
import com.kaisou.app.core.translation.GoogleTranslator

// Puts a long text of AniList in the language of the app keeping its marks, by lines in groups, giving each group as it comes back
object MarkdownTranslator {
    // How many characters and how many lines go in one request, which has to fit in an address
    private const val GroupCharacters = 2_500
    private const val GroupLines = 40
    private const val SignOpen = '⟦'
    private const val SignClose = '⟧'

    // What makes a line a heading, a quote or an item, which stays out of the translation
    private val lead = Regex("""^\s*(?:#{1,6}\s+|>\s*|[-*+]\s+|\d+[.)]\s+)*""")

    // The marks inside a line: pictures, videos, links, addresses, center, spoiler, strike, bold, italic and tags
    private val marks = Regex("""(?i)(?:img\d*%?|youtube|webm)\([^)]*\)|!\[[^\]]*]\([^)]*\)|]\([^)]*\)|https?://\S+|~~~|~!|!~|~~|__|\*\*|\*|(?<![\p{L}\p{N}])_|_(?![\p{L}\p{N}])|</?[a-z][^>]*>""")
    private val signs = Regex("""$SignOpen\s*(\d+)\s*$SignClose""")
    private val letters = Regex("""\p{L}""")

    // Short texts of one line each in the language given, staying as written if they do not come back one by one
    suspend fun translateLines(texts: List<String>, language: String): List<String> {
        if (texts.isEmpty()) return texts
        val lines = translate(texts.joinToString("\n") { it.replace('\n', ' ') }, language).lines()
        return if (lines.size == texts.size) lines.mapIndexed { index, line -> line.ifBlank { texts[index] } } else texts
    }

    // The line that sets one text apart from the next when several go together. It has no words, so it is not sent to be translated
    private const val Separator = "\u27E6\u00B6\u27E7"

    // Several texts in the language given, together, calling onPartial per group, unchanged if they do not come back apart
    suspend fun translateAll(texts: List<String>, language: String, onPartial: (List<String>) -> Unit = {}): List<String> {
        if (texts.isEmpty()) return texts
        val glue = "\n$Separator\n"

        // Splits a joined translation back into the texts, null when the count differs
        fun apart(joined: String): List<String>? = joined.split(glue).takeIf { it.size == texts.size }
        val done = translate(texts.joinToString(glue), language) { partial -> apart(partial)?.let(onPartial) }
        return apart(done) ?: texts
    }

    // One line that is to be translated: what comes before its words, the line with its marks changed for signs, and the marks
    private class Piece(val index: Int, val lead: String, val body: String, val marks: List<String>)

    // The text in the language given. onPartial is called with the whole text each time a group is done, in the order it was written
    suspend fun translate(text: String, language: String, onPartial: (String) -> Unit = {}): String = coroutineScope {
        val lines = text.split("\n").toMutableList()
        val pieces = lines.mapIndexedNotNull { index, line -> pieceOf(index, line) }
        val groups = mutableListOf<MutableList<Piece>>()
        var size = 0
        for (piece in pieces) {
            if (groups.isEmpty() || size + piece.body.length > GroupCharacters || groups.last().size >= GroupLines) {
                groups += mutableListOf<Piece>()
                size = 0
            }
            groups.last() += piece
            size += piece.body.length
        }
        groups.map { group ->
            async {
                val done = translateGroup(group.map { it.body }, language)
                synchronized(lines) {
                    group.forEachIndexed { at, piece ->
                        val body = done[at]
                        val back = putBack(body, piece.marks)
                        if (back != null) lines[piece.index] = piece.lead + back
                    }
                    onPartial(lines.joinToString("\n"))
                }
            }
        }.awaitAll()
        lines.joinToString("\n")
    }

    // The line made a piece to translate, null when it has no words, as a blank line, a rule or a picture alone
    private fun pieceOf(index: Int, line: String): Piece? {
        val start = lead.find(line)?.value.orEmpty()
        val rest = line.substring(start.length)
        val found = ArrayList<String>()
        val body = marks.replace(rest) { mark ->
            found += mark.value
            "$SignOpen${found.size - 1}$SignClose"
        }
        if (!letters.containsMatchIn(signs.replace(body, ""))) return null
        return Piece(index, start, body, found)
    }

    // The lines of a group in the language, in the same order. A group that does not come back with all its lines is cut in two
    private suspend fun translateGroup(group: List<String>, language: String): List<String> {
        val back = GoogleTranslator.translate(group.joinToString("\n"), language)?.lines()
        if (back != null && back.size == group.size) return back
        if (group.size == 1) return listOf(back?.joinToString(" ") ?: group[0])
        val half = group.size / 2
        return coroutineScope {
            val first = async { translateGroup(group.subList(0, half), language) }
            val second = async { translateGroup(group.subList(half, group.size), language) }
            first.await() + second.await()
        }
    }

    // Puts the marks back in a translated line, null when one was lost or changed so the line is not used
    private fun putBack(body: String, marks: List<String>): String? {
        val seen = HashSet<Int>()
        var broken = false
        val text = signs.replace(body) { sign ->
            val number = sign.groupValues[1].toInt()
            if (number !in marks.indices || !seen.add(number)) {
                broken = true
                ""
            } else {
                marks[number]
            }
        }
        return if (broken || seen.size != marks.size) null else text
    }
}
