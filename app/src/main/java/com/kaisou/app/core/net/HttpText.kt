package com.kaisou.app.core.net

// Java networking and streams
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL

// A plain GET that answers with the text of the page. The request blocks the thread, so it is made from a background one
object HttpText {
    // How long to wait for an answer and the name the app gives itself
    private const val TimeoutMillis = 15_000
    private const val UserAgent = "Kaisou"

    // The text of the answer, null when the service does not know it, TooManyRequests when it asks for a rest, IOException on failure
    fun get(address: String, timeoutMillis: Int = TimeoutMillis, headers: Map<String, String> = emptyMap()): String? =
        read(address, timeoutMillis, headers) { it.readText() }

    // Gives the answer to a reader while it comes, for the ones too big to keep, null when unknown, errors as in get
    fun <T> read(address: String, timeoutMillis: Int = TimeoutMillis, headers: Map<String, String> = emptyMap(), block: (Reader) -> T): T? {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", UserAgent)
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            return when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> InputStreamReader(connection.inputStream, Charsets.UTF_8).use(block)
                HttpURLConnection.HTTP_NOT_FOUND -> null
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("${connection.url.host} answered ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }
}
