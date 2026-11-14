package com.kaisou.app.core.artwork

// Chooses the backdrop of a title, the wide picture that fills the screen lying down, the way the poster is chosen: the sharp
// ones with the shape of a screen, the ones with no language first, since the picture is clean, then the ones in the language of
// the app, then the English ones and, when there are none of those, the ones in any other language, since a sharp backdrop is
// better than the poster stretched. In each group the one with the most pixels wins, the rating of the site settles a tie. Null
// when no backdrop is good enough
object BackdropPicker {
    // The narrowest backdrop that is still sharp when it fills the screen, and how wide against tall it has to be to be a wide
    // picture and not a banner
    const val MinWidth = 1920
    const val MinRatio = 1.6f
    const val MaxRatio = 2.1f

    private const val English = "en"

    fun pick(backdrops: List<Artwork>, language: String): Artwork? {
        val sharp = backdrops.filter {
            it.width >= MinWidth && it.height > 0 && it.width.toFloat() / it.height in MinRatio..MaxRatio
        }
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
