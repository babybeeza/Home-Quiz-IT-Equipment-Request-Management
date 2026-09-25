package com.example.equipment.domain

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class EquipmentType {
    NOTEBOOK,
    MONITOR,
    KEYBOARD,
    MOUSE,
    HEADSET,
    OTHER,
}

data class EquipmentItemDraft(
    val equipmentType: EquipmentType,
    val quantity: Int,
    val specification: String? = null,
)

data class EquipmentRequestDraft(
    val employeeName: String,
    val employeeEmail: String,
    val department: String,
    val title: String,
    val purpose: String,
    val requiredDate: LocalDate,
    val additionalNote: String? = null,
    val items: List<EquipmentItemDraft> = emptyList(),
)

class EquipmentRequestValidator(
    private val clock: Clock,
    private val businessZone: ZoneId = ZoneId.of("Asia/Bangkok"),
) {
    fun validateDraft(draft: EquipmentRequestDraft): Map<String, String> = buildMap {
        requireTrimmedLength("employeeName", draft.employeeName, 2, 100)
        validateEmail(draft.employeeEmail)
        requireTrimmedLength("department", draft.department, 1, 100)
        requireTrimmedLength("title", draft.title, 5, 150)
        requireTrimmedLength("purpose", draft.purpose, 10, 500)

        if (draft.requiredDate.isBefore(LocalDate.now(clock.withZone(businessZone)))) {
            put("requiredDate", "Required date must not be in the past")
        }
        if (draft.additionalNote != null && draft.additionalNote.length > 500) {
            put("additionalNote", "Additional note must not exceed 500 characters")
        }

        draft.items.forEachIndexed { index, item ->
            if (item.quantity !in 1..5) {
                put("items[$index].quantity", "Quantity must be between 1 and 5")
            }
            if (item.specification != null && item.specification.length > 250) {
                put("items[$index].specification", "Specification must not exceed 250 characters")
            }
        }
    }

    fun validateForSubmit(draft: EquipmentRequestDraft): Map<String, String> =
        validateDraft(draft).toMutableMap().apply {
            if (draft.items.isEmpty()) {
                put("items", "At least one equipment item is required before submit")
            }
        }

    fun validateRejectionReason(reason: String?): Map<String, String> = buildMap {
        val normalized = reason?.trim().orEmpty()
        if (normalized.isEmpty()) {
            put("reason", "Rejection reason is required")
        } else if (normalized.length > 500) {
            put("reason", "Rejection reason must not exceed 500 characters")
        }
    }

    private fun MutableMap<String, String>.requireTrimmedLength(
        field: String,
        value: String,
        minimum: Int,
        maximum: Int,
    ) {
        if (value.trim().length !in minimum..maximum) {
            put(field, "$field must contain between $minimum and $maximum characters")
        }
    }

    private fun MutableMap<String, String>.validateEmail(value: String) {
        val normalized = value.trim()
        if (normalized.length > 254 || !EMAIL_PATTERN.matches(normalized)) {
            put("employeeEmail", "Email format is invalid")
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
