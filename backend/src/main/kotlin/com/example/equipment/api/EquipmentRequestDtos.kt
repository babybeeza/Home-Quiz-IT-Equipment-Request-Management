package com.example.equipment.api

import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class EquipmentItemInput(
    val equipmentType: EquipmentType,
    @field:Min(1)
    @field:Max(5)
    val quantity: Int,
    @field:Size(max = 250)
    val specification: String? = null,
)

data class CreateEquipmentRequestBody(
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val employeeName: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val employeeEmail: String,
    @field:NotBlank
    @field:Size(max = 100)
    val department: String,
    @field:NotBlank
    @field:Size(min = 5, max = 150)
    val title: String,
    @field:NotBlank
    @field:Size(min = 10, max = 500)
    val purpose: String,
    val requiredDate: LocalDate,
    @field:Size(max = 500)
    val additionalNote: String? = null,
    val items: List<@Valid EquipmentItemInput>,
)

data class UpdateEquipmentRequestBody(
    @field:NotBlank
    @field:Size(min = 2, max = 100)
    val employeeName: String,
    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    val employeeEmail: String,
    @field:NotBlank
    @field:Size(max = 100)
    val department: String,
    @field:NotBlank
    @field:Size(min = 5, max = 150)
    val title: String,
    @field:NotBlank
    @field:Size(min = 10, max = 500)
    val purpose: String,
    val requiredDate: LocalDate,
    @field:Size(max = 500)
    val additionalNote: String? = null,
    val items: List<@Valid EquipmentItemInput>,
    @field:Min(0)
    val expectedVersion: Long,
)

data class EquipmentItemResponse(
    val id: UUID,
    val equipmentType: EquipmentType,
    val quantity: Int,
    val specification: String?,
)

data class EquipmentRequestResponse(
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
    val items: List<EquipmentItemResponse>,
    val totalItems: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)


data class VersionActionBody(
    val expectedVersion: Long,
)

data class RejectActionBody(
    val expectedVersion: Long,
    // Nullable so a missing reason reaches the service and returns 422 after version/state checks (ADR-004).
    val reason: String? = null,
)

data class EquipmentRequestSummaryResponse(
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

data class EquipmentRequestPageResponse(
    val content: List<EquipmentRequestSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
