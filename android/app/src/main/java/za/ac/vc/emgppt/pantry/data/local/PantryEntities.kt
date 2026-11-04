package za.ac.vc.emgppt.pantry.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local pantry row. Room is the offline source of truth until the API
 * acknowledges the write (Android Developers, 2024).
 */
@Entity(tableName = "pantry_items")
data class PantryItemEntity(
    @PrimaryKey val id: String,
    val ingredientName: String,
    val quantity: Double?,
    val unit: String?,
    val imageUrl: String?,
    val createdAt: String?,
    val updatedAt: String?
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceRecipeIdsJson: String,
    val itemsJson: String,
    val createdAt: String?,
    val updatedAt: String?
)

@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val kind: String,
    val entityId: String,
    val payloadJson: String,
    val createdAt: Long
)

@Entity(tableName = "recipe_cache")
data class RecipeCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payloadJson: String
)
