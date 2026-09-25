package com.example.equipment.application

import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestEntity
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.NewEquipmentItem
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs the ADR-005 search against real PostgreSQL 17 with Flyway V1 and its trigram/B-tree indexes. */
@SpringBootTest(properties = ["spring.jpa.properties.hibernate.generate_statistics=true"])
@Testcontainers(disabledWithoutDocker = true)
class EquipmentRequestSearchIntegrationTest {
    @Autowired
    private lateinit var queryService: EquipmentRequestQueryService

    @Autowired
    private lateinit var repository: EquipmentRequestRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    private var sequence = 0

    @BeforeEach
    fun cleanDatabase() {
        jdbc.execute("truncate table equipment_requests cascade")
    }

    @Test
    fun `employees see only their own requests and totals while approvers see all`() {
        insert(owner = ALICE)
        insert(owner = ALICE, status = RequestStatus.PENDING)
        insert(owner = BOB)

        val alice = queryService.search(ALICE, RequestSearchCriteria())
        assertEquals(2, alice.totalElements)
        assertTrue(alice.content.all { it.employeeName == "Alice Owner" })
        assertEquals(1, queryService.search(BOB, RequestSearchCriteria()).totalElements)
        assertEquals(3, queryService.search(APPROVER, RequestSearchCriteria()).totalElements)
    }

    @Test
    fun `keyword matches number, title and employee name case-insensitively and literally`() {
        val byNumber = insert(number = "REQ-2026-777001")
        val byTitle = insert(title = "Ergonomic keyboard for design team")
        val byName = insert(name = "Kanokwan Srisuk")
        val percent = insert(title = "100% wireless mouse set")
        val underscore = insert(title = "USB_C docking hub")
        val thai = insert(title = "จอภาพสำหรับงานออกแบบกราฟิก")
        insert(title = "USBxC adapter placeholder")

        assertEquals(listOf(byNumber), ids("777001"))
        assertEquals(listOf(byTitle), ids("ERGONOMIC"))
        assertEquals(listOf(byName), ids("kanok"))
        assertEquals(listOf(percent), ids("%"))
        assertEquals(listOf(underscore), ids("B_C"))
        assertEquals(listOf(thai), ids("จอภาพ"))
    }

    @Test
    fun `keyword, status and department narrow together`() {
        val match = insert(title = "Monitor upgrade", status = RequestStatus.PENDING, department = "Software Engineering")
        insert(title = "Monitor upgrade", status = RequestStatus.DRAFT, department = "Software Engineering")
        insert(title = "Monitor upgrade", status = RequestStatus.PENDING, department = "Finance")
        insert(title = "Keyboard upgrade", status = RequestStatus.PENDING, department = "Software Engineering")

        val result = queryService.search(
            APPROVER,
            RequestSearchCriteria(keyword = "monitor", status = RequestStatus.PENDING, department = "software engineering"),
        )

        assertEquals(listOf(match), result.content.map { it.id })
        assertEquals(1, result.totalElements)
    }

    @Test
    fun `pagination metadata is exact and both sorts visit every row once despite equal timestamps`() {
        val sameInstant = Instant.parse("2026-09-20T01:00:00Z")
        val inserted = (1..23).map { insert(createdAt = if (it <= 12) sameInstant else sameInstant.plusSeconds(it.toLong())) }

        val pages = (0..3).map { queryService.search(APPROVER, RequestSearchCriteria(page = it, size = 10)) }
        assertEquals(listOf(10, 10, 3, 0), pages.map { it.content.size })
        assertTrue(pages.all { it.totalElements == 23L && it.totalPages == 3 })

        val descending = pages.flatMap { page -> page.content.map { it.id } }
        val ascending = (0..2).flatMap { page ->
            queryService.search(APPROVER, RequestSearchCriteria(page = page, size = 10, order = CreatedAtOrder.ASC))
                .content.map { it.id }
        }
        assertEquals(inserted.toSet(), descending.toSet())
        assertEquals(23, descending.distinct().size)
        assertEquals(descending.reversed(), ascending)

        val empty = queryService.search(APPROVER, RequestSearchCriteria(keyword = "no such request"))
        assertEquals(0, empty.totalElements)
        assertEquals(0, empty.totalPages)
    }

    @Test
    fun `totalItems sums quantities with a bounded statement count`() {
        val withItems = insert(
            items = listOf(NewEquipmentItem(EquipmentType.MONITOR, 2, null), NewEquipmentItem(EquipmentType.MOUSE, 3, null)),
        )
        val withoutItems = insert(items = emptyList())
        repeat(8) { insert() }
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.clear()

        val page = queryService.search(APPROVER, RequestSearchCriteria(size = 10))

        val totals = page.content.associate { it.id to it.totalItems }
        assertEquals(5, totals[withItems])
        assertEquals(0, totals[withoutItems])
        assertEquals(10, page.content.size)
        // Page rows, count and one grouped totals query; never one query per request.
        assertTrue(statistics.prepareStatementCount <= 3, "statements: ${statistics.prepareStatementCount}")
    }

    private fun ids(keyword: String) =
        queryService.search(APPROVER, RequestSearchCriteria(keyword = keyword)).content.map { it.id }

    private fun insert(
        owner: Actor = ALICE,
        number: String? = null,
        title: String = "Standard equipment request",
        name: String? = null,
        department: String = "Operations",
        status: RequestStatus = RequestStatus.DRAFT,
        createdAt: Instant = Instant.parse("2026-09-01T00:00:00Z").plusSeconds((++sequence).toLong()),
        items: List<NewEquipmentItem> = listOf(NewEquipmentItem(EquipmentType.NOTEBOOK, 1, null)),
    ): UUID {
        val entity = EquipmentRequestEntity(
            id = UUID.randomUUID(),
            requestNumber = number ?: "REQ-2026-%06d".format(100000 + (++sequence)),
            ownerId = owner.userId,
            employeeName = name ?: if (owner == BOB) "Bob Other" else "Alice Owner",
            employeeEmail = "someone@example.com",
            department = department,
            title = title,
            purpose = "Integration test fixture purpose",
            requiredDate = LocalDate.of(2099, 1, 1),
            status = status,
            rejectionReason = if (status == RequestStatus.REJECTED) "Fixture reason" else null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        entity.replaceItems(items)
        return repository.saveAndFlush(entity).id
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")

        private val ALICE = Actor("employee-001", ActorRole.EMPLOYEE)
        private val BOB = Actor("employee-002", ActorRole.EMPLOYEE)
        private val APPROVER = Actor("approver-001", ActorRole.APPROVER)
    }
}
