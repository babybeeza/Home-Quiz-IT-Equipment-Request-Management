package com.example.equipment.persistence

import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "equipment_requests")
class EquipmentRequestEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "request_number", nullable = false, unique = true, length = 32)
    var requestNumber: String = "",

    @Column(name = "owner_id", nullable = false, length = 100)
    var ownerId: String = "",

    @Column(name = "employee_name", nullable = false, length = 100)
    var employeeName: String = "",

    @Column(name = "employee_email", nullable = false, length = 254)
    var employeeEmail: String = "",

    @Column(nullable = false, length = 100)
    var department: String = "",

    @Column(nullable = false, length = 150)
    var title: String = "",

    @Column(nullable = false, length = 500)
    var purpose: String = "",

    @Column(name = "required_date", nullable = false)
    var requiredDate: LocalDate = LocalDate.now(),

    @Column(name = "additional_note", length = 500)
    var additionalNote: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: RequestStatus = RequestStatus.DRAFT,

    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null,

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @OneToMany(mappedBy = "request", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<EquipmentRequestItemEntity> = mutableListOf()

    fun replaceItems(newItems: List<NewEquipmentItem>) {
        items.clear()
        newItems.forEachIndexed { index, item ->
            // No position column exists; one microsecond per index keeps submitted order stable at PostgreSQL precision.
            val timestamp = updatedAt.plusNanos(index * 1_000L)
            items += EquipmentRequestItemEntity(
                request = this,
                equipmentType = item.equipmentType,
                quantity = item.quantity.toShort(),
                specification = item.specification,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
    }
}

data class NewEquipmentItem(
    val equipmentType: EquipmentType,
    val quantity: Int,
    val specification: String?,
)

@Entity
@Table(name = "equipment_request_items")
class EquipmentRequestItemEntity(
    @Id
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    var request: EquipmentRequestEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false, length = 16)
    var equipmentType: EquipmentType = EquipmentType.OTHER,

    @Column(nullable = false)
    var quantity: Short = 1,

    @Column(length = 250)
    var specification: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

