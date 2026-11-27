package com.kaisou.app.features.user.data

// Android framework
import android.content.Context
// Project resources
import com.kaisou.app.R

// The category each tag is put in, read once from the file in the resources, one line for each tag with its category and name
object TagCategories {
    // The category of the tags that no line names
    const val Other = "Format"

    @Volatile
    private var byTag: Map<String, String>? = null

    // Gives the category of each tag by its name
    fun of(context: Context): Map<String, String> = byTag ?: synchronized(this) {
        byTag ?: context.resources.openRawResource(R.raw.tag_categories).bufferedReader().useLines { lines ->
            lines.mapNotNull { line -> line.split('\t').takeIf { it.size == 2 }?.let { it[1] to it[0] } }.toMap()
        }.also { byTag = it }
    }
}
