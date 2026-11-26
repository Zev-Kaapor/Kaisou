package com.kaisou.app.core.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Collections

// Tests of the batcher that puts the ids asked together into one question
class IdBatcherTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // The ids asked within the wait go in one question, each one asked once
    @Test
    fun theIdsAskedTogetherGoInOneQuestion() {
        val questions = Collections.synchronizedList(mutableListOf<List<Int>>())
        val batcher = IdBatcher(size = 50, waitMillis = 100, scope = scope) { ids -> questions += ids; ids.associateWith { it * 10 } }
        val answers = runBlocking { listOf(1, 2, 3, 2, 1).map { id -> async { batcher.get(id)?.get(id) } }.awaitAll() }
        assertEquals(listOf(10, 20, 30, 20, 10), answers)
        assertEquals(1, questions.size)
        assertEquals(listOf(1, 2, 3), questions.single().sorted())
    }

    // More ids than a question holds go in as many questions as needed
    @Test
    fun moreIdsThanAQuestionHoldsGoInSeveralQuestions() {
        val questions = Collections.synchronizedList(mutableListOf<List<Int>>())
        val batcher = IdBatcher(size = 2, waitMillis = 100, scope = scope) { ids -> questions += ids; ids.associateWith { it } }
        val answers = runBlocking { (1..5).map { id -> async { batcher.get(id)?.get(id) } }.awaitAll() }
        assertEquals((1..5).toList(), answers)
        assertEquals(listOf(2, 2, 1), questions.map { it.size })
    }

    // A question that failed gives nothing to everyone who waited for it
    @Test
    fun aQuestionThatFailedGivesNothing() {
        val batcher = IdBatcher<Int>(size = 50, waitMillis = 50, scope = scope) { null }
        assertNull(runBlocking { batcher.get(7) })
    }
}
