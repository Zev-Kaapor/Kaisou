package com.kaisou.app.core.artwork

// Chooses the logos of a title worth trying, in the order to try them: the language of the app first, since the name is
// written on it, then English, then the ones with no language and, last, any other
object LogoPicker {
    // The narrowest logo that is still sharp at the size it is shown
    const val MinWidth = 400
    // The ones taller than wide are left out, stacked on a short line they would come out tiny
    const val MinAspect = 1f
    // How many are kept, the first ones can be turned down when they are drawn in black
    const val MaxCandidates = 3

    private const val English = "en"
    // The vector ones cannot be decoded
    private const val Vector = ".svg"

    // Addresses, best first. In each language the one with the most pixels comes first, the rating settles a tie
    fun pick(logos: List<Artwork>, language: String): List<String> {
        val usable = logos.filter { it.width >= MinWidth && it.width >= it.height * MinAspect && !it.url.endsWith(Vector, ignoreCase = true) }
        val groups = listOf<(Artwork) -> Boolean>(
            { it.language == language },
            { it.language == English },
            { it.language == null },
            { true }
        )
        val bestFirst = compareByDescending<Artwork> { it.width.toLong() * it.height }.thenByDescending { it.score }
        return groups
            .flatMap { group -> usable.filter(group).sortedWith(bestFirst) }
            .map { it.url }
            .distinct()
            .take(MaxCandidates)
    }
}
