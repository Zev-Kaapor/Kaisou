package com.kaisou.app.core.artwork

// Chooses the poster of a title among the ones the sites have, the sharpest one of the best language
object PosterPicker {
    // The narrowest poster that is still sharp when it fills the screen, and how much taller than wide it has to be to be a poster
    const val MinWidth = 1000

    // The narrowest a poster of a season can be, since TheTVDB has them at 680 wide
    const val SeasonMinWidth = 600
    const val MinHeightRatio = 1.2f

    private const val English = "en"

    // The small and the ones that are not standing up are left out. Of the rest, the ones with no language come first, since
    // the picture is clean, then the ones in the language of the app, then the English ones and, when there are none of
    // those, the ones in any other language, since a sharp poster is better than the cover. In each group the one with the
    // most pixels wins, the rating of the site settles a tie. Null when no poster is good enough
    fun pick(posters: List<Artwork>, language: String, minWidth: Int = MinWidth): Artwork? {
        val sharp = posters.filter { it.width >= minWidth && it.height >= it.width * MinHeightRatio }
        val groups = listOf<(Artwork) -> Boolean>(
            { it.language == null },
            { it.language == language },
            { it.language == English },
            { true }
        )
        for (group in groups) {
            val found = sharp.filter(group)
            if (found.isNotEmpty()) {
                return found.maxWith(compareBy<Artwork> { it.width.toLong() * it.height }.thenBy { it.score })
            }
        }
        return null
    }
}
