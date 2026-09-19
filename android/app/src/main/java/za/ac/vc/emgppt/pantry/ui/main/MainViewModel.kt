package za.ac.vc.emgppt.pantry.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.CreatePantryItemRequest
import za.ac.vc.emgppt.pantry.data.api.IngredientDto
import za.ac.vc.emgppt.pantry.data.api.PantryItemDto
import za.ac.vc.emgppt.pantry.data.api.RecipeDetailDto
import za.ac.vc.emgppt.pantry.data.api.RecipeMatchRequest
import za.ac.vc.emgppt.pantry.data.api.RecipeSummaryDto
import za.ac.vc.emgppt.pantry.data.api.UserSettings

data class MainUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val pantry: List<PantryItemDto> = emptyList(),
    val recipes: List<RecipeSummaryDto> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val ingredientSuggestions: List<IngredientDto> = emptyList(),
    val recipeDetail: RecipeDetailDto? = null,
    val searchQuery: String = ""
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    fun refreshAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            try {
                val pantry = ServiceLocator.api.listPantry().data.orEmpty()
                val settings = ServiceLocator.api.getSettings().data ?: UserSettings()
                val recipes = ServiceLocator.api.matchRecipes(RecipeMatchRequest()).data.orEmpty()
                _state.value = _state.value.copy(
                    loading = false,
                    pantry = pantry,
                    settings = settings,
                    recipes = recipes
                )
                Log.i("Pantry", "Loaded pantry=${pantry.size} recipes=${recipes.size}")
            } catch (ex: Exception) {
                Log.e("Pantry", "refreshAll failed", ex)
                _state.value = _state.value.copy(loading = false, message = ex.message ?: "Failed to load data.")
            }
        }
    }

    fun searchIngredients(query: String) {
        viewModelScope.launch {
            try {
                val results = ServiceLocator.api.searchIngredients(query).data.orEmpty()
                _state.value = _state.value.copy(ingredientSuggestions = results)
            } catch (ex: Exception) {
                Log.w("Pantry", "Ingredient search failed: ${ex.message}")
            }
        }
    }

    fun addPantryItem(name: String, quantity: String, unit: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(message = "Ingredient name is required.")
            return
        }
        val qty = quantity.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (quantity.isNotBlank() && qty == null) {
            _state.value = _state.value.copy(message = "Quantity must be a number.")
            return
        }
        if (qty != null && qty < 0) {
            _state.value = _state.value.copy(message = "Quantity cannot be negative.")
            return
        }
        viewModelScope.launch {
            try {
                val created = ServiceLocator.api.addPantryItem(
                    CreatePantryItemRequest(trimmed, qty, unit.trim().ifBlank { null })
                )
                if (created.success) {
                    Log.i("Pantry", "Added pantry item ${created.data?.ingredientName}")
                    refreshAll()
                } else {
                    _state.value = _state.value.copy(message = created.error)
                }
            } catch (ex: Exception) {
                _state.value = _state.value.copy(message = ex.message)
            }
        }
    }

    fun deletePantryItem(id: String) {
        viewModelScope.launch {
            try {
                ServiceLocator.api.deletePantryItem(id)
                refreshAll()
            } catch (ex: Exception) {
                _state.value = _state.value.copy(message = ex.message)
            }
        }
    }

    fun searchRecipes(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        viewModelScope.launch {
            try {
                val results = if (query.isBlank()) {
                    ServiceLocator.api.matchRecipes(RecipeMatchRequest()).data.orEmpty()
                } else {
                    ServiceLocator.api.listRecipes(query).data.orEmpty()
                }
                _state.value = _state.value.copy(recipes = results)
            } catch (ex: Exception) {
                _state.value = _state.value.copy(message = ex.message)
            }
        }
    }

    fun loadRecipe(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val detail = ServiceLocator.api.getRecipe(id).data
                _state.value = _state.value.copy(loading = false, recipeDetail = detail)
            } catch (ex: Exception) {
                _state.value = _state.value.copy(loading = false, message = ex.message)
            }
        }
    }

    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            try {
                val saved = ServiceLocator.api.updateSettings(settings)
                if (saved.success && saved.data != null) {
                    _state.value = _state.value.copy(settings = saved.data, message = "Settings saved.")
                    Log.i("Pantry", "Settings saved: diets=${saved.data.dietaryPreferences}")
                    refreshAll()
                } else {
                    _state.value = _state.value.copy(message = saved.error)
                }
            } catch (ex: Exception) {
                _state.value = _state.value.copy(message = ex.message)
            }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
