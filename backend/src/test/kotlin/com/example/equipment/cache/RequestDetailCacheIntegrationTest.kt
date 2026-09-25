package com.example.equipment.cache

import com.example.equipment.application.EquipmentRequestAccessDenied
import com.example.equipment.application.EquipmentRequestService
import com.example.equipment.application.EquipmentRequestView
import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.EquipmentItemDraft
import com.example.equipment.domain.EquipmentRequestDraft
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** ADR-006 Redis detail cache against real PostgreSQL 17 and Redis 8. */
@SpringBootTest(
    properties = [
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.cache.request-detail.ttl=2s",
    ],
)
@Testcontainers(disabledWithoutDocker = true)
class RequestDetailCacheIntegrationTest {
    @Autowired
    private lateinit var service: EquipmentRequestService

    @Autowired
    private lateinit var redis: StringRedisTemplate

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var transactions: TransactionTemplate

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var registry: MeterRegistry

    @BeforeEach
    fun clean() {
        jdbc.execute("truncate table equipment_requests cascade")
        redis.keys("${RedisRequestDetailCache.KEY_PREFIX}*").takeIf { it.isNotEmpty() }?.let(redis::delete)
    }

    @Test
    fun `first read fills with a TTL, second read is a hit without the aggregate query, then the entry expires`() {
        val created = service.create(OWNER, draft())
        val key = RedisRequestDetailCache.key(created.id, 0)
        assertFalse(redis.hasKey(key), "create must not pre-fill before any read")

        service.get(OWNER, created.id)
        val ttl = redis.getExpire(key, TimeUnit.MILLISECONDS)
        assertTrue(ttl in 1..2000, "ttl=$ttl")

        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.clear()
        val hitsBefore = count("hit")
        assertEquals(created.title, service.get(OWNER, created.id).title)
        assertEquals(1, statistics.prepareStatementCount, "hit should run only the owner/version lookup")
        assertEquals(hitsBefore + 1, count("hit"))

        awaitUntil { !redis.hasKey(key) }
        val missesBefore = count("miss")
        service.get(OWNER, created.id)
        assertEquals(missesBefore + 1, count("miss"))
        assertTrue(redis.hasKey(key))
    }

    @Test
    fun `every mutation publishes its new version after commit and evicts the previous one`() {
        val request = service.create(OWNER, draft())
        service.get(OWNER, request.id)

        val edited = service.update(OWNER, request.id, draft(title = "Edited monitor request"), 0)
        assertCachedOnly(request.id, 1)
        assertEquals("Edited monitor request", service.get(APPROVER, request.id).title)

        val submitted = service.submit(OWNER, request.id, edited.version)
        assertCachedOnly(request.id, 2)
        assertEquals(RequestStatus.PENDING, service.get(OWNER, request.id).status)

        service.approve(APPROVER, request.id, submitted.version)
        assertCachedOnly(request.id, 3)
        service.get(OWNER, request.id).let {
            assertEquals(RequestStatus.APPROVED, it.status)
            assertEquals(3, it.version)
        }

        val toReject = service.submit(OWNER, service.create(OWNER, draft()).id, 0)
        service.reject(APPROVER, toReject.id, toReject.version, "  Over budget  ")
        assertCachedOnly(toReject.id, 2)
        assertEquals("Over budget", service.get(OWNER, toReject.id).rejectionReason)

        val toCancel = service.create(OWNER, draft())
        service.cancel(OWNER, toCancel.id, 0)
        assertCachedOnly(toCancel.id, 1)
        assertEquals(RequestStatus.CANCELLED, service.get(OWNER, toCancel.id).status)
    }

    @Test
    fun `rolled-back mutation publishes nothing and reads stay on the committed version`() {
        val request = service.create(OWNER, draft(title = "Committed title"))
        service.get(OWNER, request.id)

        transactions.executeWithoutResult { status ->
            service.update(OWNER, request.id, draft(title = "Rolled back title"), 0)
            status.setRollbackOnly()
        }

        assertFalse(redis.hasKey(RedisRequestDetailCache.key(request.id, 1)))
        service.get(OWNER, request.id).let {
            assertEquals("Committed title", it.title)
            assertEquals(0, it.version)
        }
    }

    @Test
    fun `a late fill under an old version is never served`() {
        val request = service.create(OWNER, draft(title = "Original title"))
        service.update(OWNER, request.id, draft(title = "Latest title"), 0)
        // Simulate a slow reader that loaded version 0 and wrote it back after version 1 committed.
        redis.opsForValue().set(
            RedisRequestDetailCache.key(request.id, 0),
            jsonMapper.writeValueAsString(request.copy(title = "Stale late fill")),
        )

        service.get(OWNER, request.id).let {
            assertEquals("Latest title", it.title)
            assertEquals(1, it.version)
        }
    }

    @Test
    fun `corrupt payload is replaced from the database`() {
        val request = service.create(OWNER, draft(title = "Real title"))
        val key = RedisRequestDetailCache.key(request.id, 0)
        redis.opsForValue().set(key, "{corrupt")
        val errorsBefore = count("error")

        assertEquals("Real title", service.get(OWNER, request.id).title)
        assertEquals(errorsBefore + 1, count("error"))
        val refilled = assertNotNull(redis.opsForValue().get(key))
        assertEquals("Real title", jsonMapper.readValue(refilled, EquipmentRequestView::class.java).title)
    }

    @Test
    fun `another employee is denied even when the detail is cached`() {
        val request = service.create(OWNER, draft())
        service.get(OWNER, request.id)
        assertTrue(redis.hasKey(RedisRequestDetailCache.key(request.id, 0)))

        assertFailsWith<EquipmentRequestAccessDenied> {
            service.get(Actor("employee-002", ActorRole.EMPLOYEE), request.id)
        }
        val payload = redis.opsForValue().get(RedisRequestDetailCache.key(request.id, 0))!!
        assertFalse(payload.contains("employee-001"), "cached payload must not carry owner identity")
    }

    private fun assertCachedOnly(id: UUID, version: Long) {
        assertEquals(
            setOf(RedisRequestDetailCache.key(id, version)),
            redis.keys("${RedisRequestDetailCache.KEY_PREFIX}$id:*"),
        )
    }

    private fun count(result: String) =
        registry.get("equipment.cache.request_detail").tag("result", result).counter().count()

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "condition not met within 5 s" }
            Thread.sleep(100)
        }
    }

    private fun draft(title: String = "Monitor for design review") = EquipmentRequestDraft(
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Design",
        title = title,
        purpose = "Colour-accurate review of product mockups",
        requiredDate = LocalDate.of(2099, 1, 1),
        items = listOf(EquipmentItemDraft(EquipmentType.MONITOR, 1)),
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        @Container
        @ServiceConnection(name = "redis")
        @JvmStatic
        val redisContainer: GenericContainer<*> = GenericContainer("redis:8-alpine").withExposedPorts(6379)

        private val OWNER = Actor("employee-001", ActorRole.EMPLOYEE)
        private val APPROVER = Actor("approver-001", ActorRole.APPROVER)
    }
}
