package com.example.equipment.application

import com.example.equipment.domain.Actor
import com.example.equipment.domain.EquipmentItemDraft
import com.example.equipment.domain.EquipmentRequestDraft
import com.example.equipment.domain.EquipmentRequestValidator
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestAccessPolicy
import com.example.equipment.domain.RequestOperation
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestEntity
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.NewEquipmentItem
import com.example.equipment.persistence.RequestNumberAllocator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class EquipmentRequestService(
    private val repository: EquipmentRequestRepository,
    private val numberAllocator: RequestNumberAllocator,
    private val validator: EquipmentRequestValidator,
    private val clock: Clock,
    private val businessZone: ZoneId,
) {
    @Transactional
    fun create(actor: Actor, draft: EquipmentRequestDraft): EquipmentRequestView {
        requireAccess(actor, RequestOperation.CREATE)
        validate(draft)
        val now = Instant.now(clock)
        val sequence = numberAllocator.nextSequence()
        val entity = EquipmentRequestEntity(
            id = UUID.randomUUID(),
            requestNumber = "REQ-${LocalDate.now(clock.withZone(businessZone)).year}-${sequence.toString().padStart(6, '0')}",
            ownerId = actor.userId,
            employeeName = draft.employeeName.trim(),
            employeeEmail = draft.employeeEmail.trim(),
            department = draft.department.trim(),
            title = draft.title.trim(),
            purpose = draft.purpose.trim(),
            requiredDate = draft.requiredDate,
            additionalNote = draft.additionalNote.normalized(),
            status = RequestStatus.DRAFT,
            createdAt = now,
            updatedAt = now,
        )
        entity.replaceItems(draft.items.toNewItems())
        return repository.saveAndFlush(entity).toView()
    }

    @Transactional(readOnly = true)
    fun get(actor: Actor, id: UUID): EquipmentRequestView {
        val entity = repository.findAggregateById(id) ?: throw EquipmentRequestNotFound(id)
        requireAccess(actor, RequestOperation.VIEW, entity.ownerId)
        return entity.toView()
    }

    @Transactional
    fun update(actor: Actor, id: UUID, draft: EquipmentRequestDraft, expectedVersion: Long): EquipmentRequestView {
        val entity = repository.findAggregateById(id) ?: throw EquipmentRequestNotFound(id)
        requireAccess(actor, RequestOperation.EDIT, entity.ownerId)
        if (expectedVersion < 0) {
            throw EquipmentRequestValidationFailed(mapOf("expectedVersion" to "Expected version must not be negative"))
        }
        if (entity.version != expectedVersion) {
            throw EquipmentRequestVersionConflict(expectedVersion, entity.version)
        }
        entity.status.requireEditable()

        validate(draft)
        entity.employeeName = draft.employeeName.trim()
        entity.employeeEmail = draft.employeeEmail.trim()
        entity.department = draft.department.trim()
        entity.title = draft.title.trim()
        entity.purpose = draft.purpose.trim()
        entity.requiredDate = draft.requiredDate
        entity.additionalNote = draft.additionalNote.normalized()
        entity.updatedAt = Instant.now(clock)
        entity.replaceItems(draft.items.toNewItems())
        return repository.saveAndFlush(entity).toView()
    }

    private fun validate(draft: EquipmentRequestDraft) {
        val errors = validator.validateDraft(draft)
        if (errors.isNotEmpty()) throw EquipmentRequestValidationFailed(errors)
    }

    private fun requireAccess(actor: Actor, operation: RequestOperation, ownerId: String? = null) {
        if (!RequestAccessPolicy.isAllowed(actor, operation, ownerId)) {
            throw EquipmentRequestAccessDenied()
        }
    }
}

class EquipmentRequestNotFound(val requestId: UUID) : RuntimeException("Equipment request was not found")
class EquipmentRequestAccessDenied : RuntimeException("Actor cannot access this equipment request")
class EquipmentRequestVersionConflict(val expected: Long, val actual: Long) : RuntimeException("Equipment request version is stale")
class EquipmentRequestValidationFailed(val fieldErrors: Map<String, String>) : RuntimeException("Equipment request validation failed")

data class EquipmentItemView(
    val id: UUID,
    val equipmentType: EquipmentType,
    val quantity: Int,
    val specification: String?,
)

data class EquipmentRequestView(
    val id: UUID,
    val requestNumber: String,
    val employeeName: String,
    val employeeEmail: String,
    val department: String,
    val title: String,
    val purpose: String,
    val requiredDate: LocalDate,
    val additionalNote: String?,
    val status: RequestStatus,
    val rejectionReason: String?,
    val version: Long,
    val items: List<EquipmentItemView>,
    val totalItems: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

private fun List<EquipmentItemDraft>.toNewItems() = map { NewEquipmentItem(it.equipmentType, it.quantity, it.specification.normalized()) }
private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun EquipmentRequestEntity.toView() = EquipmentRequestView(
    id = id,
    requestNumber = requestNumber,
    employeeName = employeeName,
    employeeEmail = employeeEmail,
    department = department,
    title = title,
    purpose = purpose,
    requiredDate = requiredDate,
    additionalNote = additionalNote,
    status = status,
    rejectionReason = rejectionReason,
    version = version,
    items = items.sortedBy { it.createdAt }.map {
        EquipmentItemView(it.id, it.equipmentType, it.quantity.toInt(), it.specification)
    },
    totalItems = items.sumOf { it.quantity.toInt() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)
