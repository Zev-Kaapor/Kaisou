package com.kaisou.app.core.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Tests of the reading of the answers of Google Translate
class GoogleTranslatorTest {
    // The sentences are joined in order
    @Test
    fun theSentencesAreJoinedInOrder() {
        val body = "[[[\"Primeira frase. \",\"First sentence. \",null,null,10],[\"Segunda frase.\",\"Second sentence.\",null,null,10]],null,\"en\"]"
        assertEquals("Primeira frase. Segunda frase.", GoogleTranslator.parse(body))
    }

    // An answer with no sentences is nothing
    @Test
    fun anAnswerWithNoSentencesIsNothing() {
        assertNull(GoogleTranslator.parse("[null,null,\"en\"]"))
        assertNull(GoogleTranslator.parse("[[],null,\"en\"]"))
    }

    // The second way answers with the text alone or in a pair
    @Test
    fun theSecondWayAnswersWithTheTextAloneOrInAPair() {
        assertEquals("Ola", GoogleTranslator.parseChrome("[\"Ola\"]"))
        assertEquals("Ola", GoogleTranslator.parseChrome("[[\"Ola\",\"en\"]]"))
    }
}
