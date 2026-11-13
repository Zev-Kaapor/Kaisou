package com.kaisou.app.core.artwork

// Android framework
import android.content.Context
// Compose graphics
import androidx.compose.ui.graphics.ImageBitmap
// Image loading
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
// Coroutines
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.model.ContentRating
import com.kaisou.app.core.rating.RatingRepository
import com.kaisou.app.core.translate.Translator
// Java utilities
import java.util.Locale

// What a featured title shows besides what AniList says, already downloaded so the page can show it at once.
// The poster is always there when AniList has a cover, the others are null when no source has them
class HeroArt(
    val posterUrl: String?,
    val logo: ImageBitmap?,
    // Name in the language of the device
    val title: String?,
    // Synopsis in the language of the device
    val synopsis: String?,
    // Advisory rating, in the symbols of the language of the app
    val rating: ContentRating?
)

// The request of the poster, the page and the preload build it the same way so the one that is
// downloaded first is the one that is found in the memory cache
fun posterRequest(context: Context, url: String, size: Size): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(size)
        .build()

// How much of a synopsis goes to the translator, the page only shows a few lines of it
private const val TranslatedCharacters = 600

private const val EnglishLanguage = "en"

// How many logos of a title are tried before its name in text takes their place
private const val MaxLogoTries = 3

// Looks up the artwork and the texts of a title, picks one poster at random and one logo that shows, and downloads both.
// The cover and the synopsis of AniList are the last ones in line, when the other sources have nothing
suspend fun loadHeroArt(
    context: Context,
    anilistId: Int,
    malId: Int?,
    posterSize: Size,
    query: TitleQuery,
    anilistCover: String?,
    anilistSynopsis: String
): HeroArt = coroutineScope {
    val art = TitleArtRepository.Shared.art(anilistId, query)
    val posterUrl = art.posters.randomOrNull() ?: anilistCover
    val poster = async {
        posterUrl != null &&
            SingletonImageLoader.get(context).execute(posterRequest(context, posterUrl, posterSize)) is SuccessResult
    }
    val logo = async { firstLogo(context, art.logos) }
    val synopsis = async { synopsisOf(art, anilistSynopsis) }
    // The official rating came with the artwork, the one of MyAnimeList is asked only when it did not
    val brazilian = context.resources.getBoolean(R.bool.brazilian_rating)
    val rating = async { RatingRepository.Shared.load(anilistId, malId, brazilian) }
    HeroArt(posterUrl.takeIf { poster.await() }, logo.await(), art.title, synopsis.await(), rating.await())
}

// One of the logos that shows on the page, in random order, null when none of the ones tried does
private suspend fun firstLogo(context: Context, urls: List<String>): ImageBitmap? {
    for (url in urls.shuffled().take(MaxLogoTries)) {
        TitleLogoLoader.Shared.load(context, url)?.let { return it }
    }
    return null
}

// The synopsis in the language of the device: the one TMDB has in it, then the English one of TMDB and then the one
// of AniList, the last two translated. Null when there is none, the page keeps what AniList says
private suspend fun synopsisOf(art: TitleArt, anilistSynopsis: String): String? {
    art.overview?.let { return it }
    val source = art.englishOverview ?: anilistSynopsis.takeIf { it.isNotBlank() } ?: return null
    val locale = Locale.getDefault()
    if (locale.language == EnglishLanguage) return source
    return Translator.translate(source.take(TranslatedCharacters), locale) ?: source
}
