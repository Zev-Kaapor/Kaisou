package com.kaisou.app.core.cache

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// Tests of the store on the disk
class DiskStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    // Opens a store in the temporary folder
    private fun store(): DiskStore = DiskStore(File(folder.root, "store"))

    // What was kept comes back
    @Test
    fun whatWasKeptComesBack() {
        val store = store()
        store.put("art", "154587 pt", "{\"a\":1}\nwith a line break")
        assertEquals("{\"a\":1}\nwith a line break", store.get("art", "154587 pt", 60_000L))
    }

    // What is not there is nothing
    @Test
    fun whatIsNotThereIsNothing() {
        assertNull(store().get("art", "missing", 60_000L))
        assertNull(store().getAny("art", "missing"))
    }

    // An expired one is nothing but still there for when the service does not answer
    @Test
    fun anExpiredOneIsNothingButStillThereForWhenTheServiceDoesNotAnswer() {
        val store = store()
        store.put("pool", "key", "text")
        Thread.sleep(30)
        assertNull(store.get("pool", "key", 5L))
        assertEquals("text", store.getAny("pool", "key"))
    }

    // The spaces are separate
    @Test
    fun theSpacesAreSeparate() {
        val store = store()
        store.put("a", "key", "one")
        store.put("b", "key", "two")
        assertEquals("one", store.get("a", "key", 60_000L))
        assertEquals("two", store.get("b", "key", 60_000L))
    }

    // A key with odd characters works
    @Test
    fun aKeyWithOddCharactersWorks() {
        val store = store()
        store.put("translation", "pt/../ \"quoted\" text?", "ok")
        assertEquals("ok", store.get("translation", "pt/../ \"quoted\" text?", 60_000L))
    }
}
