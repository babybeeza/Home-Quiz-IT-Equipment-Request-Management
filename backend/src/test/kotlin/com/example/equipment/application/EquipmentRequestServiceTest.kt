package com.example.equipment.application

import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.EquipmentItemDraft
import com.example.equipment.domain.EquipmentRequestDraft
import com.example.equipment.domain.EquipmentRequestValidator
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestNotEditable
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestEntity
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.RequestNumberAllocator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EquipmentRequestServiceTest {
    private val repository = mockk<EquipmentRequestRepository>()
    private val allocator = mockk<RequestNumberAllocator>()
    private val zone = ZoneId.of("Asia/Bangkok")
    private val clock = Clock.fixed(Instant.parse("2026-09-25T03:00:00Z"), zone)
    private val service = EquipmentRequestService(
        repository,
        allocator,
        EquipmentRequestValidator(clock, zone),
        clock,
        zone,
    )
    private val owner = Actor("employee-001", ActorRole.EMPLOYEE)

    @Test
    fun `create derives owner and request metadata while normalizing input`() {
        every { allocator.nextSequence() } returns 42
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        val response = service.create(owner, validDraft().copy(additionalNote = "   "))

        assertEquals("REQ-2026-000042", response.requestNumber)
        assertEquals(RequestStatus.DRAFT, response.status)
        assertEquals(0, response.version)
        assertEquals(2, response.totalItems)
        assertNull(response.additionalNote)
        verify(exactly = 1) {
            repository.saveAndFlush(match { it.ownerId == owner.userId && it.items.single().request === it })
        }
    }

    @Test
    fun `approver cannot create a draft`() {
        assertFailsWith<EquipmentRequestAccessDenied> {
            service.create(Actor("approver-001", ActorRole.APPROVER), validDraft())
        }
        verify(exactly = 0) { allocator.nextSequence() }
    }

    @Test
    fun `owner and approver can read while another employee is denied`() {
        val entity = existingEntity()
        every { repository.findAggregateById(entity.id) } returns entity

        assertEquals(entity.id, service.get(owner, entity.id).id)
        assertEquals(entity.id, service.get(Actor("approver-001", ActorRole.APPROVER), entity.id).id)
        assertFailsWith<EquipmentRequestAccessDenied> {
            service.get(Actor("employee-002", ActorRole.EMPLOYEE), entity.id)
        }
    }

    @Test
    fun `missing request returns not found before authorization`() {
        val id = UUID.randomUUID()
        every { repository.findAggregateById(id) } returns null

        assertFailsWith<EquipmentRequestNotFound> { service.get(owner, id) }
    }

    @Test
    fun `stale update is rejected without a save`() {
        val entity = existingEntity(version = 3)
        every { repository.findAggregateById(entity.id) } returns entity

        assertFailsWith<EquipmentRequestVersionConflict> {
            service.update(owner, entity.id, validDraft(), expectedVersion = 2)
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `update checks ownership before input details`() {
        val entity = existingEntity(version = 3)
        every { repository.findAggregateById(entity.id) } returns entity

        val invalid = validDraft().copy(employeeEmail = "invalid")
        assertFailsWith<EquipmentRequestAccessDenied> {
            service.update(Actor("employee-002", ActorRole.EMPLOYEE), entity.id, invalid, expectedVersion = -1)
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `owner receives field error for a negative expected version`() {
        val entity = existingEntity(version = 3)
        every { repository.findAggregateById(entity.id) } returns entity

        val failure = assertFailsWith<EquipmentRequestValidationFailed> {
            service.update(owner, entity.id, validDraft(), expectedVersion = -1)
        }
        assertTrue(failure.fieldErrors.containsKey("expectedVersion"))
    }

    @Test
    fun `item-only update replaces children and touches aggregate`() {
        val entity = existingEntity(version = 3)
        val priorUpdatedAt = entity.updatedAt
        every { repository.findAggregateById(entity.id) } returns entity
        every { repository.saveAndFlush(entity) } answers { firstArg() }

        val response = service.update(
            owner,
            entity.id,
            validDraft().copy(
                items = listOf(EquipmentItemDraft(EquipmentType.MONITOR, 4, "  27 inch  ")),
            ),
            expectedVersion = 3,
        )

        assertEquals(4, response.totalItems)
        assertEquals("27 inch", response.items.single().specification)
        assertTrue(response.updatedAt > priorUpdatedAt)
        verify(exactly = 1) { repository.saveAndFlush(entity) }
    }

    @Test
    fun `items keep submitted order when persistence returns them unordered`() {
        every { allocator.nextSequence() } returns 7
        every { repository.saveAndFlush(any()) } answers {
            firstArg<EquipmentRequestEntity>().apply { items.reverse() }
        }
        val submitted = listOf(EquipmentType.MONITOR, EquipmentType.NOTEBOOK, EquipmentType.MOUSE)

        val response = service.create(
            owner,
            validDraft().copy(items = submitted.map { EquipmentItemDraft(it, 1) }),
        )

        assertEquals(submitted, response.items.map { it.equipmentType })
    }

    @Test
    fun `non-draft update is rejected`() {
        val entity = existingEntity(status = RequestStatus.PENDING)
        every { repository.findAggregateById(entity.id) } returns entity

        assertFailsWith<RequestNotEditable> {
            service.update(owner, entity.id, validDraft(), expectedVersion = entity.version)
        }
    }

    @Test
    fun `past required date is rejected before persistence`() {
        every { allocator.nextSequence() } returns 1
        assertFailsWith<EquipmentRequestValidationFailed> {
            service.create(owner, validDraft().copy(requiredDate = LocalDate.of(2026, 9, 24)))
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    private fun validDraft() = EquipmentRequestDraft(
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Software Engineering",
        title = "Notebook for new project",
        purpose = "Develop the customer onboarding application",
        requiredDate = LocalDate.of(2026, 10, 15),
        items = listOf(EquipmentItemDraft(EquipmentType.NOTEBOOK, 2)),
    )

    private fun existingEntity(
        version: Long = 0,
        status: RequestStatus = RequestStatus.DRAFT,
    ) = EquipmentRequestEntity(
        id = UUID.randomUUID(),
        requestNumber = "REQ-2026-000001",
        ownerId = owner.userId,
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Software Engineering",
        title = "Notebook for new project",
        purpose = "Develop the customer onboarding application",
        requiredDate = LocalDate.of(2026, 10, 15),
        status = status,
        version = version,
        createdAt = Instant.parse("2026-09-24T03:00:00Z"),
        updatedAt = Instant.parse("2026-09-24T03:00:00Z"),
    ).apply {
        replaceItems(listOf(com.example.equipment.persistence.NewEquipmentItem(EquipmentType.NOTEBOOK, 2, null)))
    }
}

