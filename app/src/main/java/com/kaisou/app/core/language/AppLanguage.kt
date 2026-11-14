package com.kaisou.app.core.language

// Java utilities
import java.util.Locale

// Where Chinese is written in the traditional characters
private val TraditionalChineseCountries = setOf("TW", "HK", "MO")

// The language the app is in. For now it is the one of the phone. When the app gets its own language setting, this is the only
// place that changes, and everything that depends on the language follows it: the posters and the logos, and the translations
object AppLanguage {
    // The two letters of the language, which is what the pictures are chosen by
    val code: String get() = Locale.getDefault().language

    // The country of the user, as in BR, which is what the advisory ratings are chosen by. It can be empty
    val region: String get() = Locale.getDefault().country

    // The code the translator asks for: the ones it has under another name go to that one and the ones it does not know, such as
    // Nynorsk, go to the closest
    val translationCode: String get() = translationCodeOf(Locale.getDefault())

    // The language the texts of AniList come in
    const val SourceCode = "en"

    // Tells if what comes from AniList has to be translated
    val needsTranslation: Boolean get() = code != SourceCode

    fun translationCodeOf(locale: Locale): String = when (locale.language) {
        "zh" -> if (locale.script == "Hant" || locale.country in TraditionalChineseCountries) "zh-TW" else "zh-CN"
        "pt" -> if (locale.country == "PT") "pt-PT" else "pt"
        "nb", "nn" -> "no"
        "in" -> "id"
        "iw" -> "he"
        "fil" -> "tl"
        else -> locale.language
    }
}
