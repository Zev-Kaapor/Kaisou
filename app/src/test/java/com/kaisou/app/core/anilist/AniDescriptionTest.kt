package com.kaisou.app.core.anilist

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the conversion of the description of AniList into markdown
class AniDescriptionTest {
    // The breaks of AniList become blank lines
    @Test
    fun theBreaksOfAniListBecomeBlankLines() {
        val raw = "The adventure is over.\n<br><br>\n(Source: Crunchyroll)"
        assertEquals("The adventure is over.\n\n(Source: Crunchyroll)", AniDescription.markdown(raw))
    }

    // A single break is a line break
    @Test
    fun aSingleBreakIsALineBreak() {
        assertEquals("one\ntwo", AniDescription.markdown("one<br>two"))
        assertEquals("one\ntwo", AniDescription.markdown("one<br />two"))
    }

    // The formats become markdown
    @Test
    fun theFormatsBecomeMarkdown() {
        assertEquals("a *b* **c** ~~d~~", AniDescription.markdown("a <i>b</i> <b>c</b> <s>d</s>"))
        assertEquals("see [here](https://x.org)", AniDescription.markdown("see <a href=\"https://x.org\">here</a>"))
        assertEquals("~~~mid~~~", AniDescription.markdown("<center>mid</center>"))
    }

    // The entities are read
    @Test
    fun theEntitiesAreRead() {
        assertEquals("\"Red-Haired\" Shanks & co's", AniDescription.markdown("&quot;Red-Haired&quot; Shanks &amp; co&#39;s"))
        assertEquals("a b", AniDescription.markdown("a&nbsp;b"))
    }

    // Other tags are left out and the text is trimmed
    @Test
    fun otherTagsAreLeftOutAndTheTextIsTrimmed() {
        assertEquals("text", AniDescription.markdown("  <p>text</p>\n\n\n"))
        assertEquals("", AniDescription.markdown(null))
    }

    // Many blank lines become one
    @Test
    fun manyBlankLinesBecomeOne() {
        assertEquals("a\n\nb", AniDescription.markdown("a\n\n\n\n<br><br>\nb"))
    }

    // A translation that puts spaces in the marks is put back
    @Test
    fun aTranslationThatPutsSpacesInTheMarksIsPutBack() {
        assertEquals("Ela viu _attack on titan_ ontem", AniDescription.tidy("Ela viu _ attack on titan _ ontem"))
        assertEquals("Ele é **forte** e ~~fraco~~", AniDescription.tidy("Ele é ** forte ** e ~~ fraco ~~"))
        assertEquals("snake_case_word e 2 * 3", AniDescription.tidy("snake_case_word e 2 * 3"))
    }
}
