package com.kaisou.app.core.artwork

// Chooses the backdrop of a title: sharp and wide, with no language first, then the app's, then English, then any other
object BackdropPicker {
    // The narrowest backdrop that is still sharp, and how wide against tall it has to be
    const val MinWidth = 1920
    const val MinRatio = 1.6f
    const val MaxRatio = 2.1f

    // Code of the English language
    private const val English = "en"

    // Picks the best backdrop, null when none is good enough
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
