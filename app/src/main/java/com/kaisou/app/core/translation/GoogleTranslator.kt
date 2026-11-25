package com.kaisou.app.core.translation

// Android logging
import android.util.Log
// JSON parsing
import org.json.JSONArray
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Project resources
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests
// Java networking and utilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Translates a text with the free endpoints of Google Translate, with no key, leaving the text as it came if they fail
object GoogleTranslator {
    // The endpoint of the translation tools, and the one of the Chrome translator as a second way when the first is busy
    private const val GtxUrl = "https://translate.googleapis.com/translate_a/single"
    private const val ChromeUrl = "https://clients5.google.com/translate_a/t"

    // The endpoints turn away the default client of Android, so the request says it is a browser
    private const val UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    private const val AutoLanguage = "auto"
    private const val TimeoutMillis = 10_000
    private const val CacheMillis = 60L * 60L * 1000L
    private const val MaxEntries = 100

    // How many texts of a list go in one request
    private const val LinesPerRequest = 15
    private const val Tag = "GoogleTranslator"

    // What is kept on the disk and for how long: a month, a translation does not change
    private const val StoreSpace = "translation"
    private const val StoreMillis = 30L * 24L * 60L * 60L * 1000L

    // The translations made, with the language in the key so a change of language does not bring back an old one
    private val cache = TimedCache<String>(CacheMillis, MaxEntries)

    // The text in the language given, null when both ways fail. The language is the code the translator asks for, as in pt
    suspend fun translate(text: String, language: String): String? {
        if (text.isBlank()) return null
        val key = (language + text).hashCode()
        cache.get(key)?.let { return it.value }
        val translated = withContext(Dispatchers.IO) {
            AppStore.get(StoreSpace, language + text, StoreMillis) ?: request(text, language)?.also { AppStore.put(StoreSpace, language + text, it) }
        } ?: return null
        cache.put(key, translated)
        return translated
    }

    // Translates a list of short texts, a few in each request one per line, keeping the texts of a group that fails
    suspend fun translateLines(texts: List<String>, language: String): List<String> =
        texts.chunked(LinesPerRequest).flatMap { group ->
            val clean = group.map { it.replace('\n', ' ').trim() }
            val lines = translate(clean.joinToString("\n"), language)?.lines()?.map { it.trim() }
            if (lines == null || lines.size != group.size) {
                Log.w(Tag, "Lines not translated: ${lines?.size} lines for ${group.size} texts")
                group
            } else {
                group.mapIndexed { index, text -> lines[index].ifEmpty { text } }
            }
        }

    // The answer of the first way is a list of pieces, each with its translation first and the original after it
    fun parse(body: String): String? {
        val pieces = JSONArray(body).optJSONArray(0) ?: return null
        val text = buildString {
            for (index in 0 until pieces.length()) {
                val piece = pieces.optJSONArray(index) ?: continue
                if (!piece.isNull(0)) append(piece.getString(0))
            }
        }
        return text.trim().takeIf { it.isNotEmpty() }
    }

    // The answer of the second way is the translation, alone or with the source language in a pair
    fun parseChrome(body: String): String? {
        val first = JSONArray(body).get(0)
        val text = if (first is JSONArray) first.getString(0) else first.toString()
        return text.trim().takeIf { it.isNotEmpty() }
    }

    // Asks Google Translate for one text, trying the endpoints in order
    private fun request(text: String, language: String): String? {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val ways = listOf(
            "$GtxUrl?client=gtx&dt=t&ie=UTF-8&oe=UTF-8&sl=$AutoLanguage&tl=$language&hl=$language&q=$encoded" to ::parse,
            "$ChromeUrl?client=dict-chrome-ex&sl=$AutoLanguage&tl=$language&q=$encoded" to ::parseChrome
        )
        for ((url, read) in ways) {
            try {
                read(get(url))?.let { return it }
            } catch (error: Exception) {
                // The next way is tried
                Log.w(Tag, "Translation failed: ${error.message}")
            }
        }
        return null
    }

    // Gets an address through the gate of Google
    private fun get(url: String): String = Gates.Google.run { getOnce(url) }

    // Gets an address once and reads the answer
    private fun getOnce(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("User-Agent", UserAgent)
            if (connection.responseCode == TooManyRequests.Code) throw TooManyRequests.of(connection)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("answered ${connection.responseCode}")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            connection.disconnect()
        }
    }
}
