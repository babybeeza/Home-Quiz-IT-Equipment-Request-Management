package com.example.equipment.api

import com.example.equipment.application.CreatedAtOrder
import com.example.equipment.application.EquipmentRequestValidationFailed
import com.example.equipment.application.RequestSearchCriteria
import com.example.equipment.domain.RequestStatus

/**
 * Parses raw list query parameters (ADR-005). Every invalid value becomes a field-named
 * VALIDATION_ERROR rather than a type-mismatch MALFORMED_REQUEST; blank values mean "absent".
 */
fun parseSearchCriteria(
    keyword: String?,
    status: String?,
    department: String?,
    page: String?,
    size: String?,
    sort: String?,
): RequestSearchCriteria {
    val errors = linkedMapOf<String, String>()
    val normalizedKeyword = keyword?.trim()?.takeIf(String::isNotEmpty)
    if (normalizedKeyword != null && normalizedKeyword.length > 150) {
        errors["keyword"] = "Keyword must not exceed 150 characters"
    }
    val normalizedDepartment = department?.trim()?.takeIf(String::isNotEmpty)
    if (normalizedDepartment != null && normalizedDepartment.length > 100) {
        errors["department"] = "Department must not exceed 100 characters"
    }
    val rawStatus = status?.trim()?.takeIf(String::isNotEmpty)
    val parsedStatus = rawStatus?.let { raw -> RequestStatus.entries.firstOrNull { it.name == raw } }
    if (rawStatus != null && parsedStatus == null) {
        errors["status"] = "Status must be one of ${RequestStatus.entries.joinToString()}"
    }
    val parsedPage = page.parseIntIn("page", 0..Int.MAX_VALUE, default = 0, errors, "Page must be an integer of at least 0")
    val parsedSize = size.parseIntIn("size", 1..100, default = 10, errors, "Size must be an integer between 1 and 100")
    val order = when (sort?.trim()?.takeIf(String::isNotEmpty)) {
        null, "createdAt,desc" -> CreatedAtOrder.DESC
        "createdAt,asc" -> CreatedAtOrder.ASC
        else -> CreatedAtOrder.DESC.also { errors["sort"] = "Sort must be createdAt,asc or createdAt,desc" }
    }

    if (errors.isNotEmpty()) throw EquipmentRequestValidationFailed(errors)
    return RequestSearchCriteria(normalizedKeyword, parsedStatus, normalizedDepartment, parsedPage, parsedSize, order)
}

private fun String?.parseIntIn(
    field: String,
    range: IntRange,
    default: Int,
    errors: MutableMap<String, String>,
    message: String,
): Int {
    val raw = this?.trim()?.takeIf(String::isNotEmpty) ?: return default
    val value = raw.toIntOrNull()
    if (value == null || value !in range) {
        errors[field] = message
        return default
    }
    return value
}
