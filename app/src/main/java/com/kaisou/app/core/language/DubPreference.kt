package com.kaisou.app.core.language

// Android framework
import android.content.Context
import android.content.SharedPreferences
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Java utilities
import java.util.Locale

// The languages that AniList keeps the voices of the characters in, by the name it uses and the code of the language
enum class DubLanguage(val apiName: String, val code: String, val englishName: String) {
    Japanese("JAPANESE", "ja", "Japanese"),
    English("ENGLISH", "en", "English"),
    Portuguese("PORTUGUESE", "pt", "Portuguese"),
    Spanish("SPANISH", "es", "Spanish"),
    French("FRENCH", "fr", "French"),
    German("GERMAN", "de", "German"),
    Italian("ITALIAN", "it", "Italian"),
    Korean("KOREAN", "ko", "Korean"),
    Hebrew("HEBREW", "he", "Hebrew"),
    Hungarian("HUNGARIAN", "hu", "Hungarian");

    // The name of the language in the language of the phone
    fun displayName(): String = Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.uppercase() }

    companion object {
        fun of(name: String?): DubLanguage = entries.firstOrNull { it.apiName == name } ?: Japanese
    }
}

// The language the user wants to hear the characters in, which is the one the voices of the pages come in. It is kept on the phone and is
// Japanese until the user chooses another
object DubPreference {
    private const val FileName = "kaisou_dub"
    private const val LanguageKey = "language"

    private lateinit var preferences: SharedPreferences
    private val mutableLanguage = MutableStateFlow(DubLanguage.Japanese)
    val language: StateFlow<DubLanguage> = mutableLanguage.asStateFlow()

    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableLanguage.value = DubLanguage.of(preferences.getString(LanguageKey, null))
    }

    fun set(language: DubLanguage) {
        mutableLanguage.value = language
        preferences.edit().putString(LanguageKey, language.apiName).apply()
    }
}
