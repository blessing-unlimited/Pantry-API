package za.ac.vc.emgppt.pantry.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY ingredientName COLLATE NOCASE")
    suspend fun listPantry(): List<PantryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPantry(item: PantryItemEntity)

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deletePantry(id: String)

    @Query("DELETE FROM pantry_items")
    suspend fun clearPantry()

    @Query("SELECT * FROM shopping_lists ORDER BY name COLLATE NOCASE")
    suspend fun listShopping(): List<ShoppingListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists")
    suspend fun clearShopping()

    @Insert
    suspend fun insertOp(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops ORDER BY createdAt")
    suspend fun listOps(): List<PendingOpEntity>

    @Query("DELETE FROM pending_ops WHERE localId IN (:ids)")
    suspend fun deleteOps(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM pending_ops")
    suspend fun countOps(): Int

    @Query("DELETE FROM pending_ops")
    suspend fun clearOps()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCache(row: RecipeCacheEntity)

    @Query("SELECT * FROM recipe_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun cache(key: String): RecipeCacheEntity?

    @Query("DELETE FROM recipe_cache")
    suspend fun clearCache()
}
