package com.example.equipment.api

import com.example.equipment.application.DepartmentView
import com.example.equipment.application.EquipmentOptionView
import com.example.equipment.application.ReferenceDataService
import com.example.equipment.application.ReferenceDataView
import com.example.equipment.configuration.ApplicationConfiguration
import com.example.equipment.domain.EquipmentType
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.Test

@WebMvcTest(ReferenceDataController::class, properties = ["app.cors.allowed-origin=http://localhost:3100"])
@Import(ApplicationConfiguration::class, ReferenceDataControllerTest.Beans::class)
class ReferenceDataControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestConfiguration
    class Beans {
        @Bean
        fun referenceDataService(): ReferenceDataService = mockk {
            every { get() } returns ReferenceDataView(
                departments = listOf(DepartmentView("FINANCE", "Finance")),
                equipmentTypes = listOf(EquipmentOptionView(EquipmentType.MONITOR, "จอภาพ", 1, 5)),
            )
        }
    }

    @Test
    fun `any valid role reads reference data`() {
        listOf("employee-001" to "EMPLOYEE", "approver-001" to "APPROVER").forEach { (userId, role) ->
            mockMvc.get("/api/v1/reference-data") {
                header("X-User-Id", userId)
                header("X-Role", role)
            }.andExpect {
                status { isOk() }
                jsonPath("$.departments[0].name") { value("Finance") }
                jsonPath("$.equipmentTypes[0].type") { value("MONITOR") }
                jsonPath("$.equipmentTypes[0].label") { value("จอภาพ") }
                jsonPath("$.equipmentTypes[0].maxQuantity") { value(5) }
            }
        }
    }

    @Test
    fun `missing or unknown identity is malformed`() {
        mockMvc.get("/api/v1/reference-data") { header("X-Role", "EMPLOYEE") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_REQUEST") }
        }
        mockMvc.get("/api/v1/reference-data") {
            header("X-User-Id", "employee-001")
            header("X-Role", "ADMIN")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("MALFORMED_REQUEST") }
        }
    }
}
