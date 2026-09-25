package com.example.equipment.api

import com.example.equipment.application.EquipmentRequestView
import com.example.equipment.domain.EquipmentItemDraft
import com.example.equipment.domain.EquipmentRequestDraft

fun CreateEquipmentRequestBody.toDraft() = EquipmentRequestDraft(
    employeeName,
    employeeEmail,
    department,
    title,
    purpose,
    requiredDate,
    additionalNote,
    items.map(EquipmentItemInput::toDraft),
)

fun UpdateEquipmentRequestBody.toDraft() = EquipmentRequestDraft(
    employeeName,
    employeeEmail,
    department,
    title,
    purpose,
    requiredDate,
    additionalNote,
    items.map(EquipmentItemInput::toDraft),
)

private fun EquipmentItemInput.toDraft() = EquipmentItemDraft(equipmentType, quantity, specification)

fun EquipmentRequestView.toResponse() = EquipmentRequestResponse(
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
    items = items.map { EquipmentItemResponse(it.id, it.equipmentType, it.quantity, it.specification) },
    totalItems = totalItems,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
