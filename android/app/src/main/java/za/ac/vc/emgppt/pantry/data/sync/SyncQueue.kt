package za.ac.vc.emgppt.pantry.data.sync

/**
 * One offline write waiting to be replayed against the custom API.
 * The queue is plain Kotlin so it can be unit-tested without a device
 * (Android Developers, 2024).
 */
data class PendingOperation(
    val localId: Long,
    val kind: String,
    val entityId: String,
    val payloadJson: String,
    val createdAt: Long
)

data class SyncPlan(
    val execute: List<PendingOperation>,
    val dropLocalIds: List<Long>
)

/**
 * Collapses an outbox so the latest write for an entity wins. A delete of a
 * row that never reached the server drops both operations (Android Developers, 2024).
 */
object SyncQueue {
    const val PANTRY_UPSERT = "PANTRY_UPSERT"
    const val PANTRY_DELETE = "PANTRY_DELETE"
    const val SHOPPING_UPSERT = "SHOPPING_UPSERT"

    fun plan(operations: List<PendingOperation>): SyncPlan {
        val sorted = operations.sortedWith(compareBy({ it.createdAt }, { it.localId }))
        val latest = LinkedHashMap<String, PendingOperation>()
        val dropped = mutableListOf<Long>()

        for (op in sorted) {
            val key = when (op.kind) {
                PANTRY_UPSERT, PANTRY_DELETE -> "pantry:${op.entityId}"
                SHOPPING_UPSERT -> "shopping:${op.entityId}"
                else -> "other:${op.kind}:${op.entityId}"
            }
            val previous = latest[key]
            if (op.kind == PANTRY_DELETE && op.entityId.startsWith("local-")) {
                if (previous != null) {
                    dropped += previous.localId
                }
                dropped += op.localId
                latest.remove(key)
                continue
            }
            if (previous != null) {
                dropped += previous.localId
            }
            latest[key] = op
        }

        return SyncPlan(
            execute = latest.values.sortedWith(compareBy({ it.createdAt }, { it.localId })),
            dropLocalIds = dropped.distinct()
        )
    }
}

/** JSON body stored with a pantry outbox row. */
data class PantryWrite(
    val id: String,
    val ingredientName: String,
    val quantity: Double?,
    val unit: String?,
    val imageUrl: String?,
    val localImagePath: String?
)
