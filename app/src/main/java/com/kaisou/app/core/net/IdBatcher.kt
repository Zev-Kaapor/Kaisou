package com.kaisou.app.core.net

// Coroutines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Gathers the ids asked within a moment into questions of up to a size, so the cards that ask together make one request
class IdBatcher<V>(
    private val size: Int,
    private val waitMillis: Long,
    private val scope: CoroutineScope,
    private val ask: suspend (List<Int>) -> Map<Int, V>?
) {
    // The ids that wait for the next question, with who waits for each. The question goes when the wait of a moment is over
    private val waiting = LinkedHashMap<Int, CompletableDeferred<Map<Int, V>?>>()

    // The ids of the question that went and has no answer yet, so whoever asks for one of them waits for that answer
    private val asking = HashMap<Int, CompletableDeferred<Map<Int, V>?>>()
    private var running = false

    // The answer of the question that held the id, null when it could not be asked
    suspend fun get(id: Int): Map<Int, V>? {
        val answer = synchronized(waiting) {
            val one = asking[id] ?: waiting.getOrPut(id) { CompletableDeferred() }
            if (!running) {
                running = true
                scope.launch { runBatches() }
            }
            one
        }
        return answer.await()
    }

    // Asks for the ids that wait, up to the size at a time, until none is left
    private suspend fun runBatches() {
        delay(waitMillis)
        while (true) {
            val batch = synchronized(waiting) {
                if (waiting.isEmpty()) {
                    running = false
                    return
                }
                waiting.keys.take(size).associateWith { waiting.remove(it)!! }.also { asking.putAll(it) }
            }
            val found = ask(batch.keys.toList())
            synchronized(waiting) { asking.keys.removeAll(batch.keys) }
            batch.values.forEach { it.complete(found) }
        }
    }
}
