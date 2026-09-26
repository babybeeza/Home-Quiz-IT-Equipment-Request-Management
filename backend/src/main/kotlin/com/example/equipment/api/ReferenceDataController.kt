package com.example.equipment.api

import com.example.equipment.application.ReferenceDataService
import com.example.equipment.application.ReferenceDataView
import com.example.equipment.domain.EquipmentType
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

data class DepartmentResponse(val code: String, val name: String)

data class EquipmentOptionResponse(
    val type: EquipmentType,
    val label: String,
    val minQuantity: Int,
    val maxQuantity: Int,
)

data class ReferenceDataResponse(
    val departments: List<DepartmentResponse>,
    val equipmentTypes: List<EquipmentOptionResponse>,
)

@RestController
class ReferenceDataController(
    private val service: ReferenceDataService,
) {
    @GetMapping("/api/v1/reference-data", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
    ): ReferenceDataResponse {
        parseActor(userId, role) // same identity contract as every other endpoint; any role may read
        return service.get().toResponse()
    }
}

private fun ReferenceDataView.toResponse() = ReferenceDataResponse(
    departments = departments.map { DepartmentResponse(it.code, it.name) },
    equipmentTypes = equipmentTypes.map { EquipmentOptionResponse(it.type, it.label, it.minQuantity, it.maxQuantity) },
)
