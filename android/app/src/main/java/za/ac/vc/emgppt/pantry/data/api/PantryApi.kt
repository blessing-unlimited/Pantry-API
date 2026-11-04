package za.ac.vc.emgppt.pantry.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the custom Pantry REST API. The Android app never
 * calls TheMealDB directly so ranking stays on the server (Android Developers, 2024).
 */
interface PantryApi {
    @POST("api/v1/auth/sso")
    suspend fun sso(@Body request: SsoRequest): ApiEnvelope<SsoResponse>

    @GET("api/v1/users/me")
    suspend fun me(): ApiEnvelope<UserProfile>

    @GET("api/v1/users/me/settings")
    suspend fun getSettings(): ApiEnvelope<UserSettings>

    @PATCH("api/v1/users/me/settings")
    suspend fun updateSettings(@Body settings: UserSettings): ApiEnvelope<UserSettings>

    @PUT("api/v1/users/me/device")
    suspend fun registerDevice(@Body request: DeviceTokenRequest): ApiEnvelope<UserProfile>

    @POST("api/v1/notifications/test")
    suspend fun sendTestNotification(): ApiEnvelope<String>

    @GET("api/v1/pantry-items")
    suspend fun listPantry(): ApiEnvelope<List<PantryItemDto>>

    @POST("api/v1/pantry-items")
    suspend fun addPantryItem(@Body request: CreatePantryItemRequest): ApiEnvelope<PantryItemDto>

    @PATCH("api/v1/pantry-items/{id}")
    suspend fun updatePantryItem(
        @Path("id") id: String,
        @Body request: CreatePantryItemRequest
    ): ApiEnvelope<PantryItemDto>

    @DELETE("api/v1/pantry-items/{id}")
    suspend fun deletePantryItem(@Path("id") id: String): Response<Unit>

    @GET("api/v1/ingredients/search")
    suspend fun searchIngredients(@Query("q") query: String): ApiEnvelope<List<IngredientDto>>

    @GET("api/v1/recipes")
    suspend fun listRecipes(@Query("q") query: String? = null): ApiEnvelope<List<RecipeSummaryDto>>

    @POST("api/v1/recipes/match")
    suspend fun matchRecipes(@Body request: RecipeMatchRequest = RecipeMatchRequest()): ApiEnvelope<List<RecipeSummaryDto>>

    @GET("api/v1/recipes/{id}")
    suspend fun getRecipe(@Path("id") id: String): ApiEnvelope<RecipeDetailDto>

    @Multipart
    @POST("api/v1/food-recognition/identify")
    suspend fun identifyFood(@Part image: MultipartBody.Part): ApiEnvelope<IdentifyFoodResponse>

    @POST("api/v1/food-recognition/confirm")
    suspend fun confirmFood(@Body request: ConfirmFoodRequest): ApiEnvelope<ConfirmFoodResponse>

    @POST("api/v1/shopping-lists/generate")
    suspend fun generateShoppingList(@Body request: GenerateShoppingListRequest): ApiEnvelope<ShoppingListDto>

    @GET("api/v1/shopping-lists")
    suspend fun listShoppingLists(): ApiEnvelope<List<ShoppingListDto>>

    @GET("api/v1/shopping-lists/{id}")
    suspend fun getShoppingList(@Path("id") id: String): ApiEnvelope<ShoppingListDto>

    @PATCH("api/v1/shopping-lists/{id}")
    suspend fun updateShoppingList(
        @Path("id") id: String,
        @Body request: UpdateShoppingListRequest
    ): ApiEnvelope<ShoppingListDto>

    @GET("api/v1/substitutions/{ingredientId}")
    suspend fun substitutions(@Path("ingredientId") ingredientId: String): ApiEnvelope<List<SubstitutionDto>>
}
