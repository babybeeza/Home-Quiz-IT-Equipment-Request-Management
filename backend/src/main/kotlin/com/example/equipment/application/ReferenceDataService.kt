package com.example.equipment.application

import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.MAX_ITEM_QUANTITY
import com.example.equipment.domain.MIN_ITEM_QUANTITY
import com.example.equipment.persistence.DepartmentRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

const val REFERENCE_DATA_CACHE = "reference-data"

data class DepartmentView(val code: String, val name: String)

data class EquipmentOptionView(
    val type: EquipmentType,
    val label: String,
    val minQuantity: Int,
    val maxQuantity: Int,
)

data class ReferenceDataView(
    val departments: List<DepartmentView>,
    val equipmentTypes: List<EquipmentOptionView>,
)

private val equipmentLabels = mapOf(
    EquipmentType.NOTEBOOK to "โน้ตบุ๊ก",
    EquipmentType.MONITOR to "จอภาพ",
    EquipmentType.KEYBOARD to "คีย์บอร์ด",
    EquipmentType.MOUSE to "เมาส์",
    EquipmentType.HEADSET to "หูฟัง",
    EquipmentType.OTHER to "อื่น ๆ",
)

/**
 * Identical for every user and changed only by migration, so it lives in the local Caffeine cache (ADR-006).
 * The value is immutable, so sharing one cached instance across requests is safe.
 */
@Service
class ReferenceDataService(
    private val departments: DepartmentRepository,
) {
    @Cacheable(REFERENCE_DATA_CACHE, key = "'all'")
    @Transactional(readOnly = true)
    fun get(): ReferenceDataView = ReferenceDataView(
        departments = departments.findByActiveTrueOrderBySortOrderAsc().map { DepartmentView(it.code, it.name) },
        equipmentTypes = EquipmentType.entries.map {
            EquipmentOptionView(it, equipmentLabels.getValue(it), MIN_ITEM_QUANTITY, MAX_ITEM_QUANTITY)
        },
    )
}
