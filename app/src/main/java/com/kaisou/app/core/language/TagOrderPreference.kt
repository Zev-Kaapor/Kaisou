package com.kaisou.app.core.language

// Android framework
import android.content.Context
import android.content.SharedPreferences
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The orders the tags of a title can be shown in: the alphabetical order of the language of the phone, or from the one that most people say
// the title has to the one that fewest do, which is how AniList shows them
enum class TagOrder {
    Alphabetical, Percentage;

    companion object {
        fun of(name: String?): TagOrder = entries.firstOrNull { it.name == name } ?: Alphabetical
    }
}

// The order of the tags that the user chose, kept on the phone. It is the alphabetical one until they choose another
object TagOrderPreference {
    private const val FileName = "kaisou_tags"
    private const val OrderKey = "order"

    private lateinit var preferences: SharedPreferences
    private val mutableOrder = MutableStateFlow(TagOrder.Alphabetical)
    val order: StateFlow<TagOrder> = mutableOrder.asStateFlow()

    fun init(context: Context) {
        preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        mutableOrder.value = TagOrder.of(preferences.getString(OrderKey, null))
    }

    fun set(order: TagOrder) {
        mutableOrder.value = order
        preferences.edit().putString(OrderKey, order.name).apply()
    }
}
