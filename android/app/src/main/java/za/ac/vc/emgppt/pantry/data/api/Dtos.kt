package za.ac.vc.emgppt.pantry.data.api

data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

data class SsoRequest(
    val idToken: String,
    val displayName: String? = null,
    val email: String? = null
)

data class SsoResponse(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val isNewUser: Boolean
)

data class UserProfile(
    val userId: String,
    val email: String?,
    val displayName: String?,
    val createdAt: String?,
    val onboardingCompleted: Boolean
)

data class UserSettings(
    val languageCode: String = "en",
    val dietaryPreferences: List<String> = emptyList(),
    val cuisinePreferences: List<String> = emptyList(),
    val allergens: List<String> = emptyList(),
    val notificationsEnabled: Boolean = false,
    val maxMissingIngredients: Int = 20
)

data class PantryItemDto(
    val id: String,
    val ingredientName: String,
    val quantity: Double?,
    val unit: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class CreatePantryItemRequest(
    val ingredientName: String,
    val quantity: Double? = null,
    val unit: String? = null
)

data class RecipeIngredientDto(
    val name: String,
    val measure: String,
    val inPantry: Boolean
)

data class RecipeSummaryDto(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val category: String?,
    val area: String?,
    val matchPercent: Int,
    val pantryHaveCount: Int,
    val pantryNeedCount: Int,
    val missingIngredients: List<String> = emptyList()
)

data class RecipeDetailDto(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val category: String?,
    val area: String?,
    val tags: String?,
    val instructions: String,
    val youtubeUrl: String?,
    val sourceUrl: String?,
    val ingredients: List<RecipeIngredientDto> = emptyList(),
    val steps: List<String> = emptyList(),
    val matchPercent: Int,
    val pantryHaveCount: Int,
    val pantryNeedCount: Int
)

data class RecipeMatchRequest(
    val ingredientNames: List<String>? = null,
    val dietaryPreferences: List<String>? = null,
    val maxMissingIngredients: Int? = null,
    val query: String? = null
)

data class IngredientDto(
    val id: String,
    val name: String,
    val description: String?,
    val thumbnailUrl: String?
)
