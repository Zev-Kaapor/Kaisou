package com.kaisou.app.core.anilist

// Turns the description of a title as AniList sends it, with breaks, italic and links written as HTML, into the markdown of AniList
// that the app already shows, keeping the lines and the paragraphs the way they came
object AniDescription {
    private val LineBreak = Regex("(?i)<br\\s*/?>")
    private val Link = Regex("(?is)<a\\s[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>")
    private val AnyTag = Regex("<[^>]+>")
    private val Entity = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);")
    private val ManyBlankLines = Regex("\\n{3,}")
    private val SpaceAroundLines = Regex("[ \\t]*\\n[ \\t]*")

    // The tags that have a mark of their own, by the way they open and close
    private val Marks = listOf(
        Regex("(?i)</?(i|em)>") to "*",
        Regex("(?i)</?(b|strong)>") to "**",
        Regex("(?i)</?(s|strike|del)>") to "~~",
        Regex("(?i)</?center>") to "~~~"
    )

    private val Named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ")

    fun markdown(raw: String?): String {
        var text = raw ?: return ""
        text = LineBreak.replace(text, "\n")
        text = Link.replace(text) { "[${it.groupValues[2]}](${it.groupValues[1]})" }
        Marks.forEach { (tag, mark) -> text = tag.replace(text, mark) }
        text = AnyTag.replace(text, "")
        text = Entity.replace(text) { decode(it.groupValues[1]) ?: it.value }
        text = SpaceAroundLines.replace(text, "\n")
        return ManyBlankLines.replace(text, "\n\n").trim()
    }

    // The character of an entity like quot, #39 or #x27, null when it is not one that is known
    private fun decode(name: String): String? =
        if (name.startsWith("#")) {
            val number = if (name.startsWith("#x", ignoreCase = true)) name.drop(2).toIntOrNull(HexRadix) else name.drop(1).toIntOrNull()
            number?.takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }
        } else {
            Named[name.lowercase()]
        }

    private const val HexRadix = 16

    // A translation puts spaces inside the marks, as _ attack on titan _, and in a link, as [Name] (https://...), and then they are not marks any
    // more. The spaces are taken out
    private val SpacedPair = Regex("(\\*\\*|__|~~)[ \\t]+([^*_~\\n]+?)[ \\t]+\\1")
    private val SpacedSingle = Regex("(?<![*\\w])([*_])[ \\t]+([^*_\\n]+?)[ \\t]+\\1(?![*\\w])")

    private val SpacedLink = Regex("(\\[[^\\]\\n]+])[ \\t]*\\([ \\t]*(https?)[ \\t]*:[ \\t]*/[ \\t]*/[ \\t]*([^)\\s]*)[ \\t]*\\)")

    fun tidy(text: String): String =
        SpacedLink.replace(SpacedSingle.replace(SpacedPair.replace(text, "$1$2$1"), "$1$2$1"), "$1($2://$3)")
}
