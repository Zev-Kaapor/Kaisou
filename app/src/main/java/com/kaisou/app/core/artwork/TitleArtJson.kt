package com.kaisou.app.core.artwork

// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java time

// The art of a title as text, to keep it on the disk
object TitleArtJson {
    fun write(art: TitleArt): String =
        JSONObject()
            .put("poster", art.posterUrl ?: JSONObject.NULL)
            .put("backdrop", art.backdropUrl ?: JSONObject.NULL)
            .put("logos", JSONArray(art.logos))
            .put("certifications", JSONObject(art.certifications))
            .put("tmdbKind", art.tmdbKind ?: JSONObject.NULL)
            .put("tmdbId", art.tmdbId ?: JSONObject.NULL)
            .toString()

    // The ratings of the countries alone, as text
    fun writeCertifications(certifications: Map<String, String>): String = JSONObject(certifications).toString()

    // Null when the text is not one that was written here
    fun readCertifications(text: String): Map<String, String>? =
        try {
            val json = JSONObject(text)
            json.keys().asSequence().associateWith { json.getString(it) }
        } catch (_: Exception) {
            null
        }

    // Null when the text is not one that was written here
    fun read(text: String): TitleArt? =
        try {
            val json = JSONObject(text)
            val logos = json.getJSONArray("logos")
            val certifications = json.getJSONObject("certifications")
            TitleArt(
                posterUrl = if (json.isNull("poster")) null else json.getString("poster"),
                backdropUrl = if (json.isNull("backdrop")) null else json.getString("backdrop"),
                logos = (0 until logos.length()).map { logos.getString(it) },
                certifications = certifications.keys().asSequence().associateWith { certifications.getString(it) },
                tmdbKind = if (json.isNull("tmdbKind")) null else json.getString("tmdbKind"),
                tmdbId = if (json.isNull("tmdbId")) null else json.getInt("tmdbId")
            )
        } catch (_: Exception) {
            null
        }
}
