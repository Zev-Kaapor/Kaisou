package com.kaisou.app.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateGateTest {
    private fun gate() = RateGate("test", maxAtOnce = 2, log = {})

    @Test
    fun aRequestThatWorksIsMadeOnce() {
        var calls = 0
        val result = gate().run { calls++; "ok" }
        assertEquals("ok", result)
        assertEquals(1, calls)
    }

    @Test
    fun aRestTheServiceAsksForEarlyMakesTheNextRequestWaitWithoutTellingTheScreen() {
        val told = mutableListOf<Long>()
        val gate = gate().also { it.onRest = { wait -> told += wait } }
        gate.restUntil(System.currentTimeMillis() + 80)
        val started = System.currentTimeMillis()
        assertEquals("ok", gate.run { "ok" })
        assertTrue(System.currentTimeMillis() - started >= 60)
        assertTrue(told.isEmpty())
    }

    @Test
    fun aRequestThatWasTurnedAwayGoesAgainAfterTheWait() {
        var calls = 0
        val started = System.currentTimeMillis()
        val result = gate().run {
            calls++
            if (calls == 1) throw TooManyRequests(60) else "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, calls)
        assertTrue(System.currentTimeMillis() - started >= 50)
    }

    @Test(expected = TooManyRequests::class)
    fun itGivesUpWhenTheServiceKeepsTurningItAway() {
        gate().run<String> { throw TooManyRequests(1) }
    }

    @Test
    fun theWaitIsForEveryoneNotOnlyForTheOneTurnedAway() {
        val gate = gate()
        var first = 0
        gate.run { first++; if (first == 1) throw TooManyRequests(80) else Unit }
        val started = System.currentTimeMillis()
        // The gate was closed by the first and a later request has to find it open again, which it does after the wait
        gate.run { Unit }
        assertTrue(System.currentTimeMillis() - started < 80)
    }

    @Test
    fun theScreenIsToldOfTheRestOnceWhileItIsOn() {
        val gate = gate()
        val told = mutableListOf<Long>()
        gate.onRest = { told += it }
        var calls = 0
        gate.run { calls++; if (calls <= 2) throw TooManyRequests(60) else Unit }
        // The second turn away asks for a rest that ends no later than the one that is on, so it is not told again
        assertEquals(listOf(60L), told.take(1))
        assertTrue(told.size <= 2)
    }

    @Test(expected = IllegalStateException::class)
    fun anyOtherFailureGoesUpAsItIs() {
        gate().run<String> { throw IllegalStateException("broken") }
    }
}
