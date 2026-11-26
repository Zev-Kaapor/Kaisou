package com.kaisou.app.core.artwork

// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
// Project resources
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.net.IdBatcher

// The season before a title, asked of AniList for up to 50 titles in one question and kept in memory and on the phone
object PrequelRepository {
    // How long a question waits for others and how many titles it holds, the mark of none, and where and how long they are kept
    private const val BatchWaitMillis = 250L
    private const val BatchSize = 50
    private const val NoPrequel = 0
    private const val StoreSpace = "prequel1"
    private const val StoreMillis = 30L * 24L * 60L * 60L * 1000L
    private const val CacheMillis = 6L * 60L * 60L * 1000L
    private const val MaxEntries = 1000

    private val client = AniListClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The season before each title found, or NoPrequel for one that has none
    private val memory = TimedCache<Int>(CacheMillis, MaxEntries)

    // The titles asked within a moment go in one question
    private val batcher = IdBatcher(BatchSize, BatchWaitMillis, scope, ::ask)

    // The season before a title, null when it has none or AniList could not be reached
    suspend fun of(id: Int): Int? {
        memory.get(id)?.let { return it.value.takeIf { prequel -> prequel != NoPrequel } }
        withContext(Dispatchers.IO) { AppStore.get(StoreSpace, id.toString(), StoreMillis) }?.toIntOrNull()?.let { kept ->
            memory.put(id, kept)
            return kept.takeIf { it != NoPrequel }
        }
        return batcher.get(id)?.get(id)?.takeIf { it != NoPrequel }
    }

    // Asks for the seasons before a group of titles and keeps them, null when AniList could not be reached, which is not kept
    private suspend fun ask(ids: List<Int>): Map<Int, Int>? {
        val found = client.prequels(ids) ?: return null
        val all = ids.associateWith { found[it] ?: NoPrequel }
        withContext(Dispatchers.IO) {
            all.forEach { (id, prequel) ->
                memory.put(id, prequel)
                AppStore.put(StoreSpace, id.toString(), prequel.toString())
            }
        }
        return all
    }
}
