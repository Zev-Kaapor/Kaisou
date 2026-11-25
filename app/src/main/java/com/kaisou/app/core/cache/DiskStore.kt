package com.kaisou.app.core.cache

// Java files and hashing
import java.io.File
import java.security.MessageDigest

// Texts kept on the disk by space and key with the time they were kept, best effort, in the cache folder the system may empty
class DiskStore(private val root: File) {
    // What was kept and is not older than the life given, null when there is none or it expired
    fun get(space: String, key: String, lifeMillis: Long): String? {
        val kept = read(space, key) ?: return null
        return kept.second.takeIf { System.currentTimeMillis() - kept.first <= lifeMillis }
    }

    // Takes away everything kept in a space
    fun clear(space: String) {
        try {
            File(root, space).listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
            // It is only a cache
        }
    }

    // What was kept, however old it is, for when the service does not answer
    fun getAny(space: String, key: String): String? = read(space, key)?.second

    // Keeps a text under a space and a key, making room when there are too many files
    fun put(space: String, key: String, value: String) {
        try {
            val folder = File(root, space).apply { mkdirs() }
            File(folder, name(key)).writeText("${System.currentTimeMillis()}\n$value", Charsets.UTF_8)
            prune(folder)
        } catch (_: Exception) {
            // It is only a cache
        }
    }

    // The moment it was kept and the text
    private fun read(space: String, key: String): Pair<Long, String>? =
        try {
            val file = File(File(root, space), name(key))
            if (!file.isFile) {
                null
            } else {
                val text = file.readText(Charsets.UTF_8)
                val end = text.indexOf('\n')
                if (end < 0) null else text.substring(0, end).toLongOrNull()?.let { it to text.substring(end + 1) }
            }
        } catch (_: Exception) {
            null
        }

    // The name of the file of a key, which can hold characters a file name cannot
    private fun name(key: String): String =
        MessageDigest.getInstance("SHA-1").digest(key.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    // The oldest go when a space has too many
    private fun prune(folder: File) {
        val files = folder.listFiles() ?: return
        if (files.size <= MaxFiles) return
        files.sortedBy { it.lastModified() }.take(files.size - MaxFiles + PruneBatch).forEach { it.delete() }
    }

    // Constants of the store
    private companion object {
        // How many files the store holds and how many it removes at once when it is full
        const val MaxFiles = 400
        const val PruneBatch = 50
    }
}

// The store of the app, set up when it starts. Without it, as in the tests, nothing is kept
object AppStore {
    private var store: DiskStore? = null

    // Opens the store in the folder the app gives
    fun init(root: File) {
        store = DiskStore(root)
    }

    // Gets a text not older than the life asked, null when there is none
    fun get(space: String, key: String, lifeMillis: Long): String? = store?.get(space, key, lifeMillis)

    // Gets a text however old it is, null when there is none
    fun getAny(space: String, key: String): String? = store?.getAny(space, key)

    // Forgets everything kept in a space
    fun clear(space: String) {
        store?.clear(space)
    }

    // Keeps a text under a space and a key
    fun put(space: String, key: String, value: String) {
        store?.put(space, key, value)
    }
}
