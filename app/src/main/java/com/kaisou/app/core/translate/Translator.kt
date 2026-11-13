package com.kaisou.app.core.translate

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON
import org.json.JSONArray
// Java networking and utilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// Where Chinese is written in the traditional characters
private val TraditionalChineseCountries = setOf("TW", "HK", "MO")

// The code the translator asks for a language of the app or of the device, the ones it has under another name go to that one
// and the ones it does not know, such as Nynorsk, go to the closest
fun translationLanguageOf(locale: Locale): String = when (locale.language) {
    "zh" -> if (locale.script == "Hant" || locale.country in TraditionalChineseCountries) "zh-TW" else "zh-CN"
    "pt" -> if (locale.country == "PT") "pt-PT" else "pt"
    "nb", "nn" -> "no"
    "in" -> "id"
    "iw" -> "he"
    "fil" -> "tl"
    else -> locale.language
}

// Translates text with the free Google Translate endpoints, no key and no account, the same way as the lyrics of Wavvy
object Translator {
    // Endpoint of the translation tools, and the one of the Chrome translator as a second way when the first is busy
    private const val GtxUrl = "https://translate.googleapis.com/translate_a/single"
    private const val ChromeUrl = "https://clients5.google.com/translate_a/t"
    private const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    private const val AutoLanguage = "auto"
    private const val TimeoutMillis = 10_000

    // The text in the language of the locale, null when both ways fail. The source language is found by the translator
    suspend fun translate(text: String, locale: Locale): String? = withContext(Dispatchers.IO) {
        val target = translationLanguageOf(locale)
        for (attempt in listOf(::viaGtx, ::viaChrome)) {
            try {
                val translated = attempt(text, target)
                if (translated.isNotBlank()) return@withContext translated.trim()
            } catch (_: Exception) {
                // The next way is tried
            }
        }
        null
    }

    // Answer is a list of pieces, each with its translation first
    private fun viaGtx(text: String, target: String): String {
        val body = get(
            "$GtxUrl?client=gtx&dt=t&ie=UTF-8&oe=UTF-8&sl=$AutoLanguage&tl=$target&hl=$target&q=${encode(text)}"
        )
        val pieces = JSONArray(body).getJSONArray(0)
        return (0 until pieces.length()).joinToString("") { pieces.optJSONArray(it)?.optString(0).orEmpty() }
    }

    // Answer is the translation, alone or with the source language in a pair
    private fun viaChrome(text: String, target: String): String {
        val body = get("$ChromeUrl?client=dict-chrome-ex&sl=$AutoLanguage&tl=$target&q=${encode(text)}")
        val first = JSONArray(body).get(0)
        return if (first is JSONArray) first.getString(0) else first.toString()
    }

    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8")

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("User-Agent", UserAgent)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Translation answered ${connection.responseCode}")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            connection.disconnect()
        }
    }
}
