package za.ac.vc.emgppt.pantry.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import za.ac.vc.emgppt.pantry.data.sync.PendingOperation
import za.ac.vc.emgppt.pantry.data.sync.SyncQueue

class SyncQueueTest {
    @Test
    fun latestUpsertWinsAndEarlierWriteIsDropped() {
        val plan = SyncQueue.plan(
            listOf(
                op(1, SyncQueue.PANTRY_UPSERT, "local-1", 10),
                op(2, SyncQueue.PANTRY_UPSERT, "local-1", 20)
            )
        )
        assertEquals(listOf(2L), plan.execute.map { it.localId })
        assertEquals(listOf(1L), plan.dropLocalIds)
    }

    @Test
    fun localCreateThenDeleteIsNotSent() {
        val plan = SyncQueue.plan(
            listOf(
                op(1, SyncQueue.PANTRY_UPSERT, "local-9", 10),
                op(2, SyncQueue.PANTRY_DELETE, "local-9", 20)
            )
        )
        assertTrue(plan.execute.isEmpty())
        assertEquals(listOf(1L, 2L), plan.dropLocalIds)
    }

    @Test
    fun serverDeleteStaysInThePlan() {
        val plan = SyncQueue.plan(
            listOf(
                op(4, SyncQueue.PANTRY_UPSERT, "server-1", 10),
                op(5, SyncQueue.PANTRY_DELETE, "server-1", 30)
            )
        )
        assertEquals(SyncQueue.PANTRY_DELETE, plan.execute.single().kind)
        assertEquals("server-1", plan.execute.single().entityId)
    }

    @Test
    fun shoppingUpdatesCollapseIndependentlyOfPantry() {
        val plan = SyncQueue.plan(
            listOf(
                op(1, SyncQueue.SHOPPING_UPSERT, "list-a", 5),
                op(2, SyncQueue.PANTRY_UPSERT, "local-2", 6),
                op(3, SyncQueue.SHOPPING_UPSERT, "list-a", 7)
            )
        )
        assertEquals(listOf(2L, 3L), plan.execute.map { it.localId })
        assertEquals(listOf(1L), plan.dropLocalIds)
    }

    private fun op(id: Long, kind: String, entityId: String, at: Long) = PendingOperation(
        localId = id,
        kind = kind,
        entityId = entityId,
        payloadJson = "{}",
        createdAt = at
    )
}
