package com.kaisou.app.core.language

// Android framework
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The orders of the tags of a title: alphabetical, or from the one most people say it has to the fewest, as AniList does
enum class TagOrder {
    Alphabetical, Percentage;

    // Finds an order by its name
    companion object {
        // Gives the order of a name, alphabetical when it is not known
        fun of(name: String?): TagOrder = entries.firstOrNull { it.name == name } ?: Alphabetical
    }
}

// The order of the tags that the user chose, kept on the phone. It is the alphabetical one until they choose another
object TagOrderPreference {
    // Name of the file of the choice and the key of the order
    private const val FileName = "kaisou_tags"
    private const val OrderKey = "order"

    private lateinit var preferences: SharedPreferences
    private val mutableOrder = MutableStateFlow(TagOrder.Alphabetical)
    val order: StateFlow<TagOrder> = mutableOrder.asStateFlow()

    // Opens the file of the choice and reads the order
    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableOrder.value = TagOrder.of(preferences.getString(OrderKey, null))
    }

    // Changes the order and keeps it on the phone
    fun set(order: TagOrder) {
        mutableOrder.value = order
        preferences.edit { putString(OrderKey, order.name) }
    }
}
