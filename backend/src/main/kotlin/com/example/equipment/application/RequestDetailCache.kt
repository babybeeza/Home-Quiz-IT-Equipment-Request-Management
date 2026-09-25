package com.example.equipment.application

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

/**
 * Version-keyed detail cache (ADR-006). Implementations never throw for cache-store failures:
 * a failed read is a miss and a failed write is skipped, so PostgreSQL stays the source of truth.
 */
interface RequestDetailCache {
    fun get(id: UUID, version: Long): EquipmentRequestView?

    fun put(view: EquipmentRequestView)

    fun evict(id: UUID, version: Long)
}

object NoOpRequestDetailCache : RequestDetailCache {
    override fun get(id: UUID, version: Long): EquipmentRequestView? = null

    override fun put(view: EquipmentRequestView) = Unit

    override fun evict(id: UUID, version: Long) = Unit
}

/** Runs [action] only after the surrounding transaction commits; a rollback never runs it. */
internal fun afterCommit(action: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() = action()
            },
        )
    } else {
        // No transaction (plain unit tests): there is nothing to roll back, so publish immediately.
        action()
    }
}
