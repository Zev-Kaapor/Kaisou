package com.kaisou.app.core.artwork

// Chooses the logos worth trying, in order: the app's language, English, no language, then any other
object LogoPicker {
    // The narrowest logo that is still sharp at the size it is shown
    const val MinWidth = 400

    // The logos that are not clearly wider than tall are left out, since they come out tiny or are not logos
    const val MinAspect = 1.5f

    // How many are kept, the first ones can be turned down when they are drawn in black
    const val MaxCandidates = 3

    // Code of the English language
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
