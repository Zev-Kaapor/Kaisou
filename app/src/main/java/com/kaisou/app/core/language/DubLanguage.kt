package com.kaisou.app.core.language

// Java utilities
import java.text.Collator
import java.util.Locale

// The languages a voice of a character can come in, as AniList names them, with the code that gives the name in the language of the phone
// and the short mark of the badge
enum class DubLanguage(val englishName: String, val code: String) {
    Japanese("Japanese", "ja"),
    English("English", "en"),
    Portuguese("Portuguese", "pt"),
    Spanish("Spanish", "es"),
    French("French", "fr"),
    German("German", "de"),
    Italian("Italian", "it"),
    Korean("Korean", "ko"),
    Hebrew("Hebrew", "he"),
    Hungarian("Hungarian", "hu"),
    Chinese("Chinese", "zh"),
    Arabic("Arabic", "ar"),
    Russian("Russian", "ru"),
    Polish("Polish", "pl"),
    Dutch("Dutch", "nl"),
    Swedish("Swedish", "sv"),
    Turkish("Turkish", "tr"),
    Thai("Thai", "th"),
    Indonesian("Indonesian", "id"),
    Vietnamese("Vietnamese", "vi"),
    Hindi("Hindi", "hi"),
    Finnish("Finnish", "fi"),
    Catalan("Catalan", "ca"),
    Filipino("Filipino", "fil");

    // The name of the language in the language of the phone
    fun displayName(): String = Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    companion object {
        private val collator: Collator by lazy { Collator.getInstance(Locale.getDefault()) }

        fun of(englishName: String?): DubLanguage? = entries.firstOrNull { it.englishName.equals(englishName, ignoreCase = true) }

        // The name of a language in the language of the phone, as AniList names it when it is not one of the known
        fun nameOf(englishName: String): String = of(englishName)?.displayName() ?: englishName

        // The language a title was made in, by the country it comes from. Japanese when the country is not one that is known
        fun ofCountry(country: String?): DubLanguage = when (country?.uppercase()) {
            "KR" -> Korean
            "CN", "TW", "HK" -> Chinese
            "US", "GB" -> English
            "FR" -> French
            "DE" -> German
            "IT" -> Italian
            "ES" -> Spanish
            "BR", "PT" -> Portuguese
            else -> Japanese
        }

        // The order of the languages of the voices: the original first, in Japanese, and the others by their names in the language of the phone
        fun compare(first: String, second: String): Int {
            val firstOriginal = first.equals(Japanese.englishName, ignoreCase = true)
            val secondOriginal = second.equals(Japanese.englishName, ignoreCase = true)
            if (firstOriginal != secondOriginal) return if (firstOriginal) -1 else 1
            return collator.compare(nameOf(first), nameOf(second))
        }
    }
}
