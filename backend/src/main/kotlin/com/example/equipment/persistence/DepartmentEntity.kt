package com.example.equipment.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "departments")
class DepartmentEntity(
    @Id
    @Column(length = 32)
    var code: String = "",

    @Column(nullable = false, unique = true, length = 100)
    var name: String = "",

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true,
)

interface DepartmentRepository : JpaRepository<DepartmentEntity, String> {
    fun findByActiveTrueOrderBySortOrderAsc(): List<DepartmentEntity>
}
