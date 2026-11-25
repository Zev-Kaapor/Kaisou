package com.kaisou.app.core.artwork

// Chooses the poster of a title among the ones the sites have, the sharpest one of the best language
object PosterPicker {
    // The narrowest poster that is still sharp when it fills the screen, and how much taller than wide it has to be to be a poster
    const val MinWidth = 1000

    // The narrowest a poster of a season can be, since TheTVDB has them at 680 wide
    const val SeasonMinWidth = 600
    const val MinHeightRatio = 1.2f

    // Code of the English language
    private const val English = "en"

    // Leaves out the small and the lying ones, then prefers no language, the app's, English, any other, then the most pixels
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
