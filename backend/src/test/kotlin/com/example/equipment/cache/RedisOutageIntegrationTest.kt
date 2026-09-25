package com.example.equipment.cache

import com.example.equipment.application.EquipmentRequestService
import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.EquipmentItemDraft
import com.example.equipment.domain.EquipmentRequestDraft
import com.example.equipment.domain.EquipmentType
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** ADR-006 failure policy: with Redis gone, detail reads and mutations still succeed from PostgreSQL within the timeout. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RedisOutageIntegrationTest {
    @Autowired
    private lateinit var service: EquipmentRequestService

    @Autowired
    private lateinit var registry: MeterRegistry

    @Test
    fun `reads and mutations fall back to the database when redis is down`() {
        val owner = Actor("employee-001", ActorRole.EMPLOYEE)
        val request = service.create(owner, draft())
        service.get(owner, request.id) // warm the entry while Redis is up

        redis.stop()
        val errorsBefore = errors()

        val started = System.nanoTime()
        val read = service.get(owner, request.id)
        val elapsed = Duration.ofNanos(System.nanoTime() - started)

        assertEquals(request.title, read.title)
        assertTrue(elapsed < Duration.ofSeconds(1), "fallback read took $elapsed")
        assertTrue(errors() > errorsBefore, "the failed Redis read must be counted")

        val edited = service.update(owner, request.id, draft(title = "Edited while Redis is down"), 0)
        assertEquals(1, edited.version)
        assertEquals("Edited while Redis is down", service.get(owner, request.id).title)
    }

    private fun errors() = registry.get("equipment.cache.request_detail").tag("result", "error").counter().count()

    private fun draft(title: String = "Headset for support calls") = EquipmentRequestDraft(
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Operations",
        title = title,
        purpose = "Clear audio for customer support calls",
        requiredDate = LocalDate.of(2099, 1, 1),
        items = listOf(EquipmentItemDraft(EquipmentType.HEADSET, 1)),
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        @Container
        @ServiceConnection(name = "redis")
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer("redis:8-alpine").withExposedPorts(6379)
    }
}
