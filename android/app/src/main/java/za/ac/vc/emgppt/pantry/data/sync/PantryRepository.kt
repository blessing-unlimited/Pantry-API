package za.ac.vc.emgppt.pantry.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import za.ac.vc.emgppt.pantry.data.SessionStore
import za.ac.vc.emgppt.pantry.data.api.CreatePantryItemRequest
import za.ac.vc.emgppt.pantry.data.api.PantryApi
import za.ac.vc.emgppt.pantry.data.api.PantryItemDto
import za.ac.vc.emgppt.pantry.data.api.RecipeSummaryDto
import za.ac.vc.emgppt.pantry.data.api.ShoppingListDto
import za.ac.vc.emgppt.pantry.data.api.UpdateShoppingListRequest
import za.ac.vc.emgppt.pantry.data.blob.ScanImageStore
import za.ac.vc.emgppt.pantry.data.local.PantryDao
import za.ac.vc.emgppt.pantry.data.local.PantryItemEntity
import za.ac.vc.emgppt.pantry.data.local.PendingOpEntity
import za.ac.vc.emgppt.pantry.data.local.RecipeCacheEntity
import za.ac.vc.emgppt.pantry.data.local.ShoppingListEntity
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Writes pantry and shopping changes to Room first, then replays the outbox
 * when the network is up (Android Developers, 2024).
 */
class PantryRepository(
    private val appContext: Context,
    private val dao: PantryDao,
    private val api: PantryApi,
    private val session: SessionStore,
    private val network: NetworkMonitor
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes = _changes.asSharedFlow()

    fun start() {
        network.start {
            if (session.token.isNullOrBlank()) {
                return@start
            }
            scope.launch {
                val synced = flush()
                if (synced > 0) {
                    Log.i("Pantry", "Replay finished count=$synced")
                    _changes.tryEmit(Unit)
                }
            }
        }
    }

    suspend fun currentPantry(): List<PantryItemDto> = dao.listPantry().map { it.toDto() }

    suspend fun currentLists(): List<ShoppingListDto> = dao.listShopping().map { it.toDto() }

    suspend fun pendingCount(): Int = dao.countOps()

    suspend fun cachedRecipes(): List<RecipeSummaryDto> {
        val row = dao.cache(RECIPE_KEY) ?: return emptyList()
        return gson.fromJson(row.payloadJson, Array<RecipeSummaryDto>::class.java)?.toList().orEmpty()
    }

    suspend fun cacheRecipes(recipes: List<RecipeSummaryDto>) {
        dao.saveCache(RecipeCacheEntity(RECIPE_KEY, gson.toJson(recipes)))
    }

    suspend fun replacePantry(items: List<PantryItemDto>) {
        dao.clearPantry()
        items.forEach { dao.upsertPantry(it.toEntity()) }
        reapplyPendingPantry()
    }

    suspend fun replaceShopping(lists: List<ShoppingListDto>) {
        dao.clearShopping()
        lists.forEach { dao.upsertList(it.toEntity()) }
        reapplyPendingShopping()
    }

    suspend fun queuePantryUpsert(
        name: String,
        quantity: Double?,
        unit: String?,
        imageBytes: ByteArray?
    ) {
        val id = "local-${UUID.randomUUID()}"
        val imagePath = imageBytes?.let { bytes ->
            val file = File(appContext.filesDir, "scans/$id.jpg")
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file.absolutePath
        }
        val now = Instant.now().toString()
        dao.upsertPantry(PantryItemEntity(id, name, quantity, unit, null, now, now))
        dao.insertOp(
            PendingOpEntity(
                kind = SyncQueue.PANTRY_UPSERT,
                entityId = id,
                payloadJson = gson.toJson(PantryWrite(id, name, quantity, unit, null, imagePath)),
                createdAt = System.currentTimeMillis()
            )
        )
        Log.i("Pantry", "Queued pantry upsert name=$name id=$id")
    }

    suspend fun queuePantryDelete(id: String) {
        dao.deletePantry(id)
        dao.insertOp(
            PendingOpEntity(
                kind = SyncQueue.PANTRY_DELETE,
                entityId = id,
                payloadJson = "{}",
                createdAt = System.currentTimeMillis()
            )
        )
        Log.i("Pantry", "Queued pantry delete id=$id")
    }

    suspend fun queueShoppingUpdate(list: ShoppingListDto) {
        dao.upsertList(list.toEntity())
        dao.insertOp(
            PendingOpEntity(
                kind = SyncQueue.SHOPPING_UPSERT,
                entityId = list.id,
                payloadJson = gson.toJson(list),
                createdAt = System.currentTimeMillis()
            )
        )
        Log.i("Pantry", "Queued shopping update id=${list.id}")
    }

    suspend fun clearUserData() {
        dao.clearPantry()
        dao.clearShopping()
        dao.clearOps()
        dao.clearCache()
        Log.i("Pantry", "Local Room cache cleared")
    }

    /**
     * @return how many outbox rows were acknowledged by the API.
     */
    suspend fun flush(): Int {
        if (!network.isOnline()) {
            Log.i("Pantry", "Sync deferred: device is offline")
            return 0
        }
        val plan = SyncQueue.plan(dao.listOps().map { it.toOperation() })
        if (plan.dropLocalIds.isNotEmpty()) {
            dao.deleteOps(plan.dropLocalIds)
            Log.i("Pantry", "Collapsed ${plan.dropLocalIds.size} superseded outbox rows")
        }
        var done = 0
        for (op in plan.execute) {
            try {
                apply(op)
                dao.deleteOps(listOf(op.localId))
                done += 1
                Log.i("Pantry", "Synced ${op.kind} ${op.entityId}")
            } catch (ex: Exception) {
                Log.e("Pantry", "Sync halted on ${op.kind} ${op.entityId}: ${ex.message}", ex)
                break
            }
        }
        return done
    }

    private suspend fun apply(op: PendingOperation) {
        when (op.kind) {
            SyncQueue.PANTRY_UPSERT -> applyPantryUpsert(op)
            SyncQueue.PANTRY_DELETE -> applyPantryDelete(op)
            SyncQueue.SHOPPING_UPSERT -> applyShopping(op)
            else -> Log.w("Pantry", "Unknown outbox kind ${op.kind}")
        }
    }

    private suspend fun applyPantryUpsert(op: PendingOperation) {
        val write = gson.fromJson(op.payloadJson, PantryWrite::class.java)
        var imageUrl = write.imageUrl
        if (imageUrl.isNullOrBlank() && !write.localImagePath.isNullOrBlank()) {
            val file = File(write.localImagePath)
            if (file.exists()) {
                imageUrl = ScanImageStore.upload(
                    appContext,
                    session.userId ?: "device",
                    file.readBytes()
                )
            }
        }
        val request = CreatePantryItemRequest(write.ingredientName, write.quantity, write.unit, imageUrl)
        if (write.id.startsWith("local-")) {
            val created = api.addPantryItem(request)
            val saved = created.data
            if (!created.success || saved == null) {
                throw IllegalStateException(created.error ?: "Pantry create failed")
            }
            dao.deletePantry(write.id)
            dao.upsertPantry(saved.toEntity())
        } else {
            val updated = api.updatePantryItem(write.id, request)
            val saved = updated.data
            if (!updated.success || saved == null) {
                throw IllegalStateException(updated.error ?: "Pantry update failed")
            }
            dao.upsertPantry(saved.toEntity())
        }
    }

    private suspend fun applyPantryDelete(op: PendingOperation) {
        if (op.entityId.startsWith("local-")) {
            dao.deletePantry(op.entityId)
            return
        }
        val response = api.deletePantryItem(op.entityId)
        if (response.isSuccessful || response.code() == 404) {
            dao.deletePantry(op.entityId)
        } else {
            throw IllegalStateException("Pantry delete failed (${response.code()})")
        }
    }

    private suspend fun applyShopping(op: PendingOperation) {
        val list = gson.fromJson(op.payloadJson, ShoppingListDto::class.java)
        val saved = api.updateShoppingList(
            list.id,
            UpdateShoppingListRequest(name = list.name, items = list.items)
        )
        val data = saved.data
        if (!saved.success || data == null) {
            throw IllegalStateException(saved.error ?: "Shopping list update failed")
        }
        dao.upsertList(data.toEntity())
    }

    private suspend fun reapplyPendingPantry() {
        for (op in dao.listOps()) {
            if (op.kind == SyncQueue.PANTRY_UPSERT && op.entityId.startsWith("local-")) {
                val write = gson.fromJson(op.payloadJson, PantryWrite::class.java)
                val now = Instant.now().toString()
                dao.upsertPantry(
                    PantryItemEntity(write.id, write.ingredientName, write.quantity, write.unit, write.imageUrl, now, now)
                )
            }
            if (op.kind == SyncQueue.PANTRY_DELETE) {
                dao.deletePantry(op.entityId)
            }
        }
    }

    private suspend fun reapplyPendingShopping() {
        for (op in dao.listOps()) {
            if (op.kind != SyncQueue.SHOPPING_UPSERT) continue
            val list = gson.fromJson(op.payloadJson, ShoppingListDto::class.java)
            dao.upsertList(list.toEntity())
        }
    }

    private fun PendingOpEntity.toOperation() = PendingOperation(localId, kind, entityId, payloadJson, createdAt)

    private fun PantryItemEntity.toDto() = PantryItemDto(id, ingredientName, quantity, unit, createdAt, updatedAt, imageUrl)

    private fun PantryItemDto.toEntity() = PantryItemEntity(
        id, ingredientName, quantity, unit, imageUrl, createdAt, updatedAt
    )

    private fun ShoppingListEntity.toDto(): ShoppingListDto {
        val itemsType = Array<za.ac.vc.emgppt.pantry.data.api.ShoppingListItemDto>::class.java
        val items = gson.fromJson(itemsJson, itemsType)?.toList().orEmpty()
        val sources = gson.fromJson(sourceRecipeIdsJson, Array<String>::class.java)?.toList().orEmpty()
        return ShoppingListDto(id, name, sources, items, createdAt, updatedAt)
    }

    private fun ShoppingListDto.toEntity() = ShoppingListEntity(
        id = id,
        name = name,
        sourceRecipeIdsJson = gson.toJson(sourceRecipeIds),
        itemsJson = gson.toJson(items),
        createdAt = createAt,
        updatedAt = updatedAt
    )

    private companion object {
        const val RECIPE_KEY = "match"
    }
}
