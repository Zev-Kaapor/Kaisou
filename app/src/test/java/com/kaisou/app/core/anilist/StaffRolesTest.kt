package com.kaisou.app.core.anilist

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests of the translation of the parts of the staff
class StaffRolesTest {
    private val roles = StaffRoles(arrayOf("Director", "Theme Song Performance"), arrayOf("Direção", "Interpretação do tema"))

    // The part is translated and what is in parentheses stays
    @Test
    fun thePartIsTranslatedAndWhatIsInParenthesesStays() {
        assertEquals("Direção", roles.translate("Director"))
        assertEquals("Interpretação do tema (ED1)", roles.translate("Theme Song Performance (ED1)"))
        assertEquals("Direção (ep 5)", roles.translate("director (ep 5)"))
    }

    // A part that is not known stays as it came
    @Test
    fun aPartThatIsNotKnownStaysAsItCame() {
        assertEquals("Mystery Role (x)", roles.translate("Mystery Role (x)"))
    }
}
