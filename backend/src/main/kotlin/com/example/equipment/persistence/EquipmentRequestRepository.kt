package com.example.equipment.persistence

import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

interface EquipmentRequestRepository : JpaRepository<EquipmentRequestEntity, UUID> {
    @EntityGraph(attributePaths = ["items"])
    fun findAggregateById(id: UUID): EquipmentRequestEntity?
}

@Component
class RequestNumberAllocator(
    private val entityManager: EntityManager,
) {
    fun nextSequence(): Long =
        (entityManager.createNativeQuery("select nextval('equipment_request_number_seq')").singleResult as Number).toLong()
}

