package za.ac.vc.emgppt.pantry.util

/**
 * Client-side helper used for unit tests and card badges. The authoritative
 * ranking still happens on the hosted API (see PantryMatcher in Pantry.Api).
 */
object PantryMatchCalculator {
    fun normalize(value: String?): String =
        value.orEmpty().trim().lowercase().replace('_', ' ')

    fun namesMatch(recipeIngredient: String, pantryIngredient: String): Boolean {
        val recipe = normalize(recipeIngredient)
        val pantry = normalize(pantryIngredient)
        if (recipe.isEmpty() || pantry.isEmpty()) return false
        return recipe == pantry || recipe.contains(pantry) || pantry.contains(recipe)
    }

    fun matchPercent(recipeIngredients: List<String>, pantryIngredients: List<String>): Int {
        if (recipeIngredients.isEmpty()) return 0
        val have = recipeIngredients.count { recipe ->
            pantryIngredients.any { pantry -> namesMatch(recipe, pantry) }
        }
        return kotlin.math.round(have * 100.0 / recipeIngredients.size).toInt()
    }
}
