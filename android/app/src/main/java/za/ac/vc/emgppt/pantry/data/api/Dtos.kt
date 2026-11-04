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
    val onboardingCompleted: Boolean,
    val fcmToken: String? = null
)

data class DeviceTokenRequest(
    val token: String
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
    val updatedAt: String?,
    val imageUrl: String? = null
)

data class CreatePantryItemRequest(
    val ingredientName: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val imageUrl: String? = null
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

data class IdentifyFoodResponse(
    val imageId: Long = 0,
    val foods: List<String> = emptyList(),
    val topMatch: IdentifyTopMatch? = null,
    val message: String? = null
)

data class IdentifyTopMatch(
    val name: String = "",
    val confidence: Double = 0.0
)

data class ConfirmFoodRequest(
    val imageId: Long,
    val selectedFood: String,
    val allowCustomName: Boolean = false
)

data class ConfirmFoodResponse(
    val imageId: Long = 0,
    val confirmedFood: String = "",
    val wasFromSuggestions: Boolean = false,
    val confidence: Double? = null,
    val message: String = ""
)

data class ShoppingListDto(
    val id: String = "",
    val name: String = "",
    val sourceRecipeIds: List<String> = emptyList(),
    val items: List<ShoppingListItemDto> = emptyList(),
    val createAt: String? = null,
    val updatedAt: String? = null
)

data class ShoppingListItemDto(
    val ingredientName: String = "",
    val quantity: Double? = null,
    val unit: String? = null,
    val isChecked: Boolean = false,
    val sourceRecipeTitles: List<String> = emptyList()
)

data class GenerateShoppingListRequest(
    val recipeIds: List<String>,
    val name: String? = null
)

data class UpdateShoppingListRequest(
    val name: String? = null,
    val items: List<ShoppingListItemDto>? = null
)

data class SubstitutionDto(
    val ingredientName: String = "",
    val alternativeName: String = "",
    val inPantry: Boolean = false,
    val ratioHint: String? = null
)
