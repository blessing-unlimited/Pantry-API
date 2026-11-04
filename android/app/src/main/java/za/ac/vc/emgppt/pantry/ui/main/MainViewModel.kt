package za.ac.vc.emgppt.pantry.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import za.ac.vc.emgppt.pantry.R
import za.ac.vc.emgppt.pantry.data.ServiceLocator
import za.ac.vc.emgppt.pantry.data.api.IngredientDto
import za.ac.vc.emgppt.pantry.data.api.PantryItemDto
import za.ac.vc.emgppt.pantry.data.api.RecipeDetailDto
import za.ac.vc.emgppt.pantry.data.api.RecipeMatchRequest
import za.ac.vc.emgppt.pantry.data.api.RecipeSummaryDto
import za.ac.vc.emgppt.pantry.data.api.ShoppingListDto
import za.ac.vc.emgppt.pantry.data.api.ShoppingListItemDto
import za.ac.vc.emgppt.pantry.data.api.SubstitutionDto
import za.ac.vc.emgppt.pantry.data.api.UserSettings
import za.ac.vc.emgppt.pantry.ui.locale.LocaleManager

data class MainUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val offline: Boolean = false,
    val pendingSyncCount: Int = 0,
    val pantry: List<PantryItemDto> = emptyList(),
    val recipes: List<RecipeSummaryDto> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val ingredientSuggestions: List<IngredientDto> = emptyList(),
    val recipeDetail: RecipeDetailDto? = null,
    val searchQuery: String = "",
    val shoppingLists: List<ShoppingListDto> = emptyList(),
    val substitutions: Map<String, List<SubstitutionDto>> = emptyMap()
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    init {
        viewModelScope.launch {
            ServiceLocator.network.online.collect { isOnline ->
                _state.value = _state.value.copy(offline = !isOnline)
                Log.d("Pantry", "Connectivity online=$isOnline")
            }
        }
        viewModelScope.launch {
            ServiceLocator.repository.changes.collect {
                Log.i("Pantry", "Background sync completed; refreshing")
                refreshAll()
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            if (!ServiceLocator.network.isOnline()) {
                Log.i("Pantry", "Offline refresh; using Room cache")
                showLocal(message = null, recipes = ServiceLocator.repository.cachedRecipes())
                return@launch
            }
            try {
                ServiceLocator.repository.flush()
                val pantry = ServiceLocator.api.listPantry().data.orEmpty()
                ServiceLocator.repository.replacePantry(pantry)
                val settings = ServiceLocator.api.getSettings().data ?: UserSettings()
                LocaleManager.apply(settings.languageCode)
                val recipes = ServiceLocator.api.matchRecipes(RecipeMatchRequest()).data.orEmpty()
                ServiceLocator.repository.cacheRecipes(recipes)
                val lists = ServiceLocator.api.listShoppingLists().data.orEmpty()
                ServiceLocator.repository.replaceShopping(lists)
                _state.value = _state.value.copy(
                    loading = false,
                    pantry = ServiceLocator.repository.currentPantry(),
                    settings = settings,
                    recipes = recipes,
                    shoppingLists = ServiceLocator.repository.currentLists(),
                    offline = false,
                    pendingSyncCount = ServiceLocator.repository.pendingCount()
                )
                Log.i("Pantry", "Loaded pantry=${pantry.size} recipes=${recipes.size}")
            } catch (ex: Exception) {
                Log.e("Pantry", "refreshAll failed", ex)
                val cached = ServiceLocator.repository.cachedRecipes()
                showLocal(
                    message = text(R.string.offline_banner),
                    recipes = cached.ifEmpty { _state.value.recipes }
                )
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

    fun addPantryItem(name: String, quantity: String, unit: String, imageBytes: ByteArray? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.value = _state.value.copy(message = text(R.string.ingredient_required))
            return
        }
        val qty = quantity.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (quantity.isNotBlank() && qty == null) {
            _state.value = _state.value.copy(message = text(R.string.quantity_number))
            return
        }
        if (qty != null && qty < 0) {
            _state.value = _state.value.copy(message = text(R.string.quantity_negative))
            return
        }
        viewModelScope.launch {
            ServiceLocator.repository.queuePantryUpsert(trimmed, qty, unit.trim().ifBlank { null }, imageBytes)
            if (ServiceLocator.network.isOnline()) {
                try {
                    val synced = ServiceLocator.repository.flush()
                    Log.i("Pantry", "Pantry upsert flush count=$synced")
                    refreshAll()
                    return@launch
                } catch (ex: Exception) {
                    Log.e("Pantry", "Online pantry write fell back to Room", ex)
                }
            }
            showLocal(message = text(R.string.saved_offline))
        }
    }

    fun deletePantryItem(id: String) {
        viewModelScope.launch {
            ServiceLocator.repository.queuePantryDelete(id)
            if (ServiceLocator.network.isOnline()) {
                ServiceLocator.repository.flush()
                refreshAll()
            } else {
                Log.i("Pantry", "Pantry delete queued offline id=$id")
                showLocal(message = text(R.string.saved_offline))
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
                if (query.isBlank()) {
                    ServiceLocator.repository.cacheRecipes(results)
                }
                _state.value = _state.value.copy(recipes = results, offline = !ServiceLocator.network.isOnline())
            } catch (ex: Exception) {
                val cached = if (query.isBlank()) ServiceLocator.repository.cachedRecipes() else emptyList()
                _state.value = _state.value.copy(
                    recipes = cached.ifEmpty { _state.value.recipes },
                    offline = true,
                    message = if (cached.isEmpty()) ex.message else text(R.string.offline_banner)
                )
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
        val max = settings.maxMissingIngredients
        if (max !in 0..20) {
            _state.value = _state.value.copy(message = text(R.string.max_missing_invalid))
            return
        }
        LocaleManager.apply(settings.languageCode)
        viewModelScope.launch {
            if (!ServiceLocator.network.isOnline()) {
                _state.value = _state.value.copy(
                    settings = settings,
                    offline = true,
                    message = text(R.string.saved_offline)
                )
                return@launch
            }
            try {
                val saved = ServiceLocator.api.updateSettings(settings)
                if (saved.success && saved.data != null) {
                    _state.value = _state.value.copy(settings = saved.data, message = text(R.string.settings_saved))
                    Log.i("Pantry", "Settings saved: language=${saved.data.languageCode} diets=${saved.data.dietaryPreferences}")
                    refreshAll()
                } else {
                    _state.value = _state.value.copy(message = saved.error)
                }
            } catch (ex: Exception) {
                _state.value = _state.value.copy(message = ex.message)
            }
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            if (!ServiceLocator.network.isOnline()) {
                _state.value = _state.value.copy(offline = true, message = text(R.string.offline_banner))
                return@launch
            }
            try {
                val response = ServiceLocator.api.sendTestNotification()
                if (response.success) {
                    Log.i("Pantry", "Test notification requested")
                    _state.value = _state.value.copy(message = response.data ?: text(R.string.test_notification_sent))
                } else {
                    _state.value = _state.value.copy(message = response.error)
                }
            } catch (ex: HttpException) {
                val raw = ex.response()?.errorBody()?.string().orEmpty()
                val friendly = Regex(""""error"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.getOrNull(1)
                Log.w("Pantry", "Test notification failed: ${ex.code()}")
                _state.value = _state.value.copy(message = friendly ?: text(R.string.test_notification_unavailable))
            } catch (ex: Exception) {
                Log.e("Pantry", "Test notification failed", ex)
                _state.value = _state.value.copy(message = text(R.string.test_notification_unavailable))
            }
        }
    }

    fun showMessage(message: String) {
        _state.value = _state.value.copy(message = message)
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun loadShoppingLists() {
        viewModelScope.launch {
            if (!ServiceLocator.network.isOnline()) {
                showLocal(message = null)
                return@launch
            }
            try {
                val lists = ServiceLocator.api.listShoppingLists().data.orEmpty()
                ServiceLocator.repository.replaceShopping(lists)
                _state.value = _state.value.copy(
                    shoppingLists = ServiceLocator.repository.currentLists(),
                    pendingSyncCount = ServiceLocator.repository.pendingCount()
                )
            } catch (ex: Exception) {
                Log.e("Pantry", "loadShoppingLists failed", ex)
                showLocal(message = text(R.string.offline_banner))
            }
        }
    }

    fun generateShoppingList(recipeId: String, recipeTitle: String, onCreated: () -> Unit) {
        viewModelScope.launch {
            if (!ServiceLocator.network.isOnline()) {
                _state.value = _state.value.copy(offline = true, message = text(R.string.shopping_needs_network))
                return@launch
            }
            try {
                val created = ServiceLocator.api.generateShoppingList(
                    za.ac.vc.emgppt.pantry.data.api.GenerateShoppingListRequest(listOf(recipeId), "Missing for $recipeTitle")
                )
                if (created.success && created.data != null) {
                    val lists = listOf(created.data) + _state.value.shoppingLists.filter { it.id != created.data.id }
                    ServiceLocator.repository.replaceShopping(lists)
                    _state.value = _state.value.copy(
                        shoppingLists = ServiceLocator.repository.currentLists(),
                        message = ServiceLocator.appContext.getString(R.string.shopping_list_saved, created.data.items.size)
                    )
                    onCreated()
                } else {
                    _state.value = _state.value.copy(message = created.error ?: text(R.string.shopping_list_failed))
                }
            } catch (ex: Exception) {
                _state.value = _state.value.copy(message = ex.message)
            }
        }
    }

    fun toggleShoppingItem(listId: String, target: ShoppingListItemDto, checked: Boolean) {
        viewModelScope.launch {
            val current = _state.value.shoppingLists.firstOrNull { it.id == listId }
                ?: ServiceLocator.repository.currentLists().firstOrNull { it.id == listId }
                ?: return@launch
            val updatedItems = current.items.map { item ->
                if (item.ingredientName == target.ingredientName && item.quantity == target.quantity) {
                    item.copy(isChecked = checked)
                } else {
                    item
                }
            }
            val updated = current.copy(items = updatedItems)
            ServiceLocator.repository.queueShoppingUpdate(updated)
            if (ServiceLocator.network.isOnline()) {
                ServiceLocator.repository.flush()
            } else {
                Log.i("Pantry", "Shopping tick queued offline list=$listId")
            }
            _state.value = _state.value.copy(
                shoppingLists = ServiceLocator.repository.currentLists(),
                pendingSyncCount = ServiceLocator.repository.pendingCount(),
                offline = !ServiceLocator.network.isOnline(),
                message = if (ServiceLocator.network.isOnline()) null else text(R.string.saved_offline)
            )
        }
    }

    fun loadSubstitutions(names: List<String>) {
        viewModelScope.launch {
            val map = mutableMapOf<String, List<SubstitutionDto>>()
            names.distinct().take(6).forEach { name ->
                try {
                    val alts = ServiceLocator.api.substitutions(name).data.orEmpty()
                    if (alts.isNotEmpty()) map[name] = alts
                } catch (ex: Exception) {
                    Log.w("Pantry", "substitutions failed for $name: ${ex.message}")
                }
            }
            _state.value = _state.value.copy(substitutions = map)
        }
    }

    private suspend fun showLocal(message: String?, recipes: List<RecipeSummaryDto>? = null) {
        _state.value = _state.value.copy(
            loading = false,
            pantry = ServiceLocator.repository.currentPantry(),
            shoppingLists = ServiceLocator.repository.currentLists(),
            pendingSyncCount = ServiceLocator.repository.pendingCount(),
            offline = !ServiceLocator.network.isOnline(),
            recipes = recipes ?: _state.value.recipes,
            message = message
        )
    }

    private fun text(id: Int, vararg args: Any): String {
        return if (args.isEmpty()) {
            ServiceLocator.appContext.getString(id)
        } else {
            ServiceLocator.appContext.getString(id, *args)
        }
    }
}
