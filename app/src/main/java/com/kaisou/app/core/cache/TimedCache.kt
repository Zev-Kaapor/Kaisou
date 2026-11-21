package com.kaisou.app.core.cache

// Values found per id, kept for a while in least recently used order, the oldest leaves when it is full.
// An entry with an empty value means there is none, so it is not asked again
class TimedCache<V>(private val lifeMillis: Long, private val maxEntries: Int) {
    class Entry<V>(val value: V, val expiresAtMillis: Long)

    private val entries = object : LinkedHashMap<Int, Entry<V>>(0, LoadFactor, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Entry<V>>): Boolean = size > maxEntries
    }

    // The entry of an id, null when it was never kept or is no longer valid
    fun get(id: Int): Entry<V>? =
        synchronized(entries) {
            val entry = entries[id] ?: return@synchronized null
            if (entry.expiresAtMillis <= System.currentTimeMillis()) {
                entries.remove(id)
                null
            } else {
                entry
            }
        }

    fun put(id: Int, value: V) {
        synchronized(entries) { entries[id] = Entry(value, System.currentTimeMillis() + lifeMillis) }
    }

    // Forgets what was kept for an id, when it is no longer the same
    fun remove(id: Int) {
        synchronized(entries) { entries.remove(id) }
    }

    private companion object {
        const val LoadFactor = 0.75f
    }
}
