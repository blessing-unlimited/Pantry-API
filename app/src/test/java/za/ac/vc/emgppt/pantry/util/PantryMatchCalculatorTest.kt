package za.ac.vc.emgppt.pantry.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryMatchCalculatorTest {
    @Test
    fun allIngredientsPresent_is100() {
        val percent = PantryMatchCalculator.matchPercent(
            listOf("Tomato", "Garlic"),
            listOf("tomato", "garlic")
        )
        assertEquals(100, percent)
    }

    @Test
    fun halfPresent_is50() {
        val percent = PantryMatchCalculator.matchPercent(
            listOf("Tomato", "Soy Sauce"),
            listOf("tomato")
        )
        assertEquals(50, percent)
    }

    @Test
    fun substringChickenMatchesChickenBreast() {
        assertTrue(PantryMatchCalculator.namesMatch("chicken breast", "chicken"))
        assertFalse(PantryMatchCalculator.namesMatch("onion", "garlic"))
    }
}
