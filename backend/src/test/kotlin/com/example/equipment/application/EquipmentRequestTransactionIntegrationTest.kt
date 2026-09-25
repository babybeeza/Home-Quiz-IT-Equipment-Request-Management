package com.example.equipment.application

import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.EquipmentItemDraft
import com.example.equipment.domain.EquipmentRequestDraft
import com.example.equipment.domain.EquipmentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Assignment backend case "Transaction Rollback when saving an item fails": a test-only constraint makes the
 * database reject HEADSET items, which the application considers valid, so the failure happens mid-aggregate.
 */
@SpringBootTest(properties = ["app.cache.request-detail.enabled=false"])
@Testcontainers(disabledWithoutDocker = true)
class EquipmentRequestTransactionIntegrationTest {
    @Autowired
    private lateinit var service: EquipmentRequestService

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun rejectHeadsetItems() {
        jdbc.execute("truncate table equipment_requests cascade")
        jdbc.execute("alter table equipment_request_items add constraint ck_test_reject_headset check (equipment_type <> 'HEADSET')")
    }

    @AfterEach
    fun dropTestConstraint() {
        jdbc.execute("alter table equipment_request_items drop constraint if exists ck_test_reject_headset")
    }

    @Test
    fun `create leaves no request when an item insert fails`() {
        assertFailsWith<DataIntegrityViolationException> {
            service.create(OWNER, draft("Rolled back create", EquipmentType.NOTEBOOK, EquipmentType.HEADSET))
        }

        assertEquals(0, count("equipment_requests"))
        assertEquals(0, count("equipment_request_items"))
    }

    @Test
    fun `update keeps the committed request and items when a replacement item fails`() {
        val committed = service.create(OWNER, draft("Committed title", EquipmentType.NOTEBOOK))

        assertFailsWith<DataIntegrityViolationException> {
            service.update(OWNER, committed.id, draft("Rolled back title", EquipmentType.MONITOR, EquipmentType.HEADSET), 0)
        }

        val stored = service.get(OWNER, committed.id)
        assertEquals("Committed title", stored.title)
        assertEquals(0, stored.version)
        assertEquals(listOf(EquipmentType.NOTEBOOK), stored.items.map { it.equipmentType })
        assertEquals(1, count("equipment_request_items"))
    }

    private fun count(table: String) = jdbc.queryForObject("select count(*) from $table", Int::class.java)

    private fun draft(title: String, vararg types: EquipmentType) = EquipmentRequestDraft(
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Operations",
        title = title,
        purpose = "Transaction rollback verification",
        requiredDate = LocalDate.of(2099, 1, 1),
        items = types.map { EquipmentItemDraft(it, 1) },
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        private val OWNER = Actor("employee-001", ActorRole.EMPLOYEE)
    }
}
