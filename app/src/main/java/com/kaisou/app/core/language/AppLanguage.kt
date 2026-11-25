package com.kaisou.app.core.language

// Java utilities
import java.util.Locale

// Where Chinese is written in the traditional characters
private val TraditionalChineseCountries = setOf("TW", "HK", "MO")

// The language the app is in, for now the one of the phone, the only place to change when the app gets its own setting
object AppLanguage {
    // The two letters of the language, which is what the pictures are chosen by
    val code: String get() = Locale.getDefault().language

    // The country of the user, as in BR, which is what the advisory ratings are chosen by. It can be empty
    val region: String get() = Locale.getDefault().country

    // The code the translator asks for, taking the ones it has under another name and the unknown ones to the closest
    val translationCode: String get() = translationCodeOf(Locale.getDefault())

    // The language the texts of AniList come in
    const val SourceCode = "en"

    // Tells if what comes from AniList has to be translated
    val needsTranslation: Boolean get() = code != SourceCode

    // Gives the translation code of a locale
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
