package com.kaisou.app.core.cache

// Values found by id, kept for a while, the least recently used leaving when it is full
class TimedCache<V>(private val lifeMillis: Long, private val maxEntries: Int) {
    // A value with the time it stops being valid
    class Entry<V>(val value: V, val expiresAtMillis: Long)

    private val entries = object : LinkedHashMap<Int, Entry<V>>(0, LoadFactor, true) {
        // Drops the oldest entry when there are more than the maximum
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

    // Keeps a value under an id for the life of the cache
    fun put(id: Int, value: V) {
        synchronized(entries) { entries[id] = Entry(value, System.currentTimeMillis() + lifeMillis) }
    }

    // Forgets what was kept for an id, when it is no longer the same
    fun remove(id: Int) {
        synchronized(entries) { entries.remove(id) }
    }

    // Forgets all that was kept
    fun clear() {
        synchronized(entries) { entries.clear() }
    }

    // Constants of the cache
    private companion object {
        // How full the map may get before it grows
        const val LoadFactor = 0.75f
    }
}
