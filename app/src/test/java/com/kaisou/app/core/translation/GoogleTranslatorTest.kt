package com.kaisou.app.core.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleTranslatorTest {
    @Test
    fun theSentencesAreJoinedInOrder() {
        val body = "[[[\"Primeira frase. \",\"First sentence. \",null,null,10],[\"Segunda frase.\",\"Second sentence.\",null,null,10]],null,\"en\"]"
        assertEquals("Primeira frase. Segunda frase.", GoogleTranslator.parse(body))
    }

    @Test
    fun anAnswerWithNoSentencesIsNothing() {
        assertNull(GoogleTranslator.parse("[null,null,\"en\"]"))
        assertNull(GoogleTranslator.parse("[[],null,\"en\"]"))
    }

    @Test
    fun theSecondWayAnswersWithTheTextAloneOrInAPair() {
        assertEquals("Ola", GoogleTranslator.parseChrome("[\"Ola\"]"))
        assertEquals("Ola", GoogleTranslator.parseChrome("[[\"Ola\",\"en\"]]"))
    }
}
