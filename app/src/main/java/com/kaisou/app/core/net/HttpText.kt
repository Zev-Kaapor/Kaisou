package com.kaisou.app.core.net

// Java networking and streams
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL

// A plain GET that answers with the text of the page. The request blocks the thread, so it is made from a background one
object HttpText {
    private const val TimeoutMillis = 15_000
    private const val UserAgent = "Kaisou"

    // The text of the answer, null when the service does not know what was asked. A service that asks for a rest throws TooManyRequests for
    // its gate, and any other failure is an IOException
    fun get(address: String, timeoutMillis: Int = TimeoutMillis, headers: Map<String, String> = emptyMap()): String? =
        read(address, timeoutMillis, headers) { it.readText() }

    // The answer given to a reader while it is still coming, for the ones too big to keep whole, and the reader may stop before the end.
    // Null when the service does not know what was asked. Errors are the ones of get
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
