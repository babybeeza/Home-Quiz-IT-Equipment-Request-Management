package com.example.equipment.application

import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.EquipmentRequestSpecifications
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class CreatedAtOrder { ASC, DESC }

/** Validated, normalized list parameters; blank text filters are already null. */
data class RequestSearchCriteria(
    val keyword: String? = null,
    val status: RequestStatus? = null,
    val department: String? = null,
    val page: Int = 0,
    val size: Int = 10,
    val order: CreatedAtOrder = CreatedAtOrder.DESC,
)

data class EquipmentRequestSummaryView(
    val id: UUID,
    val requestNumber: String,
    val title: String,
    val employeeName: String,
    val department: String,
    val requiredDate: LocalDate,
    val totalItems: Int,
    val status: RequestStatus,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class EquipmentRequestSummaryPage(
    val content: List<EquipmentRequestSummaryView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Service
class EquipmentRequestQueryService(
    private val repository: EquipmentRequestRepository,
) {
    @Transactional(readOnly = true)
    fun search(actor: Actor, criteria: RequestSearchCriteria): EquipmentRequestSummaryPage {
        // Scope comes only from identity: Employees are always limited to their own requests.
        val ownerScope = if (actor.role == ActorRole.EMPLOYEE) actor.userId else null
        val direction = if (criteria.order == CreatedAtOrder.ASC) Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(criteria.page, criteria.size, Sort.by(direction, "createdAt", "id"))

        val page = repository.findAll(
            EquipmentRequestSpecifications.search(ownerScope, criteria.keyword, criteria.status, criteria.department),
            pageable,
        )
        val totals = if (page.content.isEmpty()) {
            emptyMap()
        } else {
            repository.sumItemQuantities(page.content.map { it.id }).associate { it.requestId to it.total.toInt() }
        }

        return EquipmentRequestSummaryPage(
            content = page.content.map {
                EquipmentRequestSummaryView(
                    id = it.id,
                    requestNumber = it.requestNumber,
                    title = it.title,
                    employeeName = it.employeeName,
                    department = it.department,
                    requiredDate = it.requiredDate,
                    totalItems = totals[it.id] ?: 0,
                    status = it.status,
                    version = it.version,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
            page = criteria.page,
            size = criteria.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
