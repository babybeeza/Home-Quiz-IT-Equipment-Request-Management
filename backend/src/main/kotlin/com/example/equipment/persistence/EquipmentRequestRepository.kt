package com.example.equipment.persistence

import com.example.equipment.domain.RequestStatus
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.util.Locale
import java.util.UUID

interface EquipmentRequestRepository :
    JpaRepository<EquipmentRequestEntity, UUID>,
    JpaSpecificationExecutor<EquipmentRequestEntity> {
    @EntityGraph(attributePaths = ["items"])
    fun findAggregateById(id: UUID): EquipmentRequestEntity?

    @Query(
        """
        select new com.example.equipment.persistence.ItemQuantityTotal(i.request.id, sum(i.quantity))
        from EquipmentRequestItemEntity i
        where i.request.id in :requestIds
        group by i.request.id
        """,
    )
    fun sumItemQuantities(@Param("requestIds") requestIds: Collection<UUID>): List<ItemQuantityTotal>
}

data class ItemQuantityTotal(val requestId: UUID, val total: Long)

/** ADR-005 predicates; each is added only when its parameter is present so PostgreSQL can use the matching index. */
object EquipmentRequestSpecifications {
    fun search(
        ownerId: String?,
        keyword: String?,
        status: RequestStatus?,
        department: String?,
    ): Specification<EquipmentRequestEntity> = Specification { root, _, cb ->
        buildList {
            ownerId?.let { add(cb.equal(root.get<String>("ownerId"), it)) }
            keyword?.let {
                // Lowered columns match the lower(...) trigram indexes; the escape keeps % and _ literal.
                val pattern = "%${escapeLike(it.lowercase(Locale.ROOT))}%"
                add(
                    cb.or(
                        cb.like(cb.lower(root.get("requestNumber")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("employeeName")), pattern, LIKE_ESCAPE),
                    ),
                )
            }
            status?.let { add(cb.equal(root.get<RequestStatus>("status"), it)) }
            department?.let { add(cb.equal(cb.lower(root.get("department")), it.lowercase(Locale.ROOT))) }
        }.let { cb.and(*it.toTypedArray()) }
    }

    private const val LIKE_ESCAPE = '\\'

    private fun escapeLike(value: String) =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}

@Component
class RequestNumberAllocator(
    private val entityManager: EntityManager,
) {
    fun nextSequence(): Long =
        (entityManager.createNativeQuery("select nextval('equipment_request_number_seq')").singleResult as Number).toLong()
}
