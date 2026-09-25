package com.example.equipment.application

import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import com.example.equipment.domain.EquipmentRequestValidator
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.InvalidRequestTransition
import com.example.equipment.domain.RequestAction
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestEntity
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.NewEquipmentItem
import com.example.equipment.persistence.RequestNumberAllocator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
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

class EquipmentRequestWorkflowTest {
    private val repository = mockk<EquipmentRequestRepository>()
    private val zone = ZoneId.of("Asia/Bangkok")
    private val clock = Clock.fixed(Instant.parse("2026-09-25T03:00:00Z"), zone)
    private val service = EquipmentRequestService(
        repository,
        mockk<RequestNumberAllocator>(),
        EquipmentRequestValidator(clock, zone),
        clock,
        zone,
        NoOpRequestDetailCache,
    )

    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("statusActionMatrix")
    fun `transition matrix allows only the approved rows`(from: RequestStatus, action: RequestAction) {
        val entity = stored(status = from, version = 4)
        val expected = ALLOWED[from to action]

        if (expected == null) {
            assertFailsWith<InvalidRequestTransition> { perform(action, entity, actorFor(action), 4) }
            assertEquals(from, entity.status)
            verify(exactly = 0) { repository.saveAndFlush(any()) }
        } else {
            val response = perform(action, entity, actorFor(action), 4)
            assertEquals(expected, response.status)
            assertTrue(response.updatedAt > STORED_AT)
            verify(exactly = 1) { repository.saveAndFlush(entity) }
        }
    }

    @ParameterizedTest(name = "{0} by {1}")
    @MethodSource("actionActorMatrix")
    fun `only the owner submits or cancels and only an approver decides`(action: RequestAction, actor: Actor) {
        val entity = stored(status = if (action == RequestAction.SUBMIT) RequestStatus.DRAFT else RequestStatus.PENDING)
        val allowed = when (action) {
            RequestAction.SUBMIT, RequestAction.CANCEL -> actor == OWNER
            RequestAction.APPROVE, RequestAction.REJECT -> actor == APPROVER
        }

        if (allowed) {
            perform(action, entity, actor, entity.version)
            verify(exactly = 1) { repository.saveAndFlush(entity) }
        } else {
            assertFailsWith<EquipmentRequestAccessDenied> { perform(action, entity, actor, entity.version) }
            verify(exactly = 0) { repository.saveAndFlush(any()) }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("actions")
    fun `error precedence is access then version then state then business rule`(action: RequestAction) {
        // Wrong source state, stale version and failing business rule all at once.
        val wrongState = if (action == RequestAction.SUBMIT) RequestStatus.PENDING else RequestStatus.APPROVED
        val entity = stored(status = wrongState, version = 3, items = emptyList(), requiredDate = YESTERDAY)
        val wrongActor = if (actorFor(action) == OWNER) APPROVER else OWNER
        val failingReason = "   "

        assertFailsWith<EquipmentRequestAccessDenied> { perform(action, entity, wrongActor, 1, failingReason) }
        assertFailsWith<EquipmentRequestVersionConflict> { perform(action, entity, actorFor(action), 1, failingReason) }
        assertFailsWith<InvalidRequestTransition> { perform(action, entity, actorFor(action), 3, failingReason) }

        entity.status = if (action == RequestAction.SUBMIT) RequestStatus.DRAFT else RequestStatus.PENDING
        if (action == RequestAction.SUBMIT || action == RequestAction.REJECT) {
            assertFailsWith<EquipmentRequestBusinessRuleViolation> {
                perform(action, entity, actorFor(action), 3, failingReason)
            }
            verify(exactly = 0) { repository.saveAndFlush(any()) }
        } else {
            perform(action, entity, actorFor(action), 3, failingReason)
            verify(exactly = 1) { repository.saveAndFlush(entity) }
        }
    }

    @Test
    fun `submit without items is rejected with ITEMS_REQUIRED and nothing changes`() {
        val entity = stored(status = RequestStatus.DRAFT, items = emptyList())

        val failure = assertFailsWith<EquipmentRequestBusinessRuleViolation> { service.submit(OWNER, entity.id, 0) }

        assertEquals(ITEMS_REQUIRED, failure.code)
        assertEquals(setOf("items"), failure.fieldErrors.keys)
        assertEquals(RequestStatus.DRAFT, entity.status)
        assertEquals(STORED_AT, entity.updatedAt)
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `submit revalidates a stored date that is now in the past`() {
        val entity = stored(status = RequestStatus.DRAFT, requiredDate = YESTERDAY)

        val failure = assertFailsWith<EquipmentRequestBusinessRuleViolation> { service.submit(OWNER, entity.id, 0) }

        assertEquals(BUSINESS_RULE_VIOLATION, failure.code)
        assertTrue(failure.fieldErrors.containsKey("requiredDate"))
        assertEquals(RequestStatus.DRAFT, entity.status)
    }

    @Test
    fun `submit accepts a required date of today in the business timezone`() {
        val entity = stored(status = RequestStatus.DRAFT, requiredDate = LocalDate.now(clock.withZone(zone)))

        assertEquals(RequestStatus.PENDING, service.submit(OWNER, entity.id, 0).status)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "   ", "\t\n"])
    fun `reject requires a nonblank reason`(reason: String?) {
        val entity = stored(status = RequestStatus.PENDING)

        val failure = assertFailsWith<EquipmentRequestBusinessRuleViolation> {
            service.reject(APPROVER, entity.id, 0, reason)
        }

        assertEquals(REJECTION_REASON_REQUIRED, failure.code)
        assertTrue(failure.fieldErrors.containsKey("reason"))
        assertEquals(RequestStatus.PENDING, entity.status)
        assertNull(entity.rejectionReason)
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `reject reason over 500 characters is a validation error`() {
        val entity = stored(status = RequestStatus.PENDING)

        val failure = assertFailsWith<EquipmentRequestValidationFailed> {
            service.reject(APPROVER, entity.id, 0, "x".repeat(501))
        }

        assertTrue(failure.fieldErrors.containsKey("reason"))
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `reject stores the trimmed reason and other actions leave it empty`() {
        val rejected = stored(status = RequestStatus.PENDING)
        assertEquals("Budget exceeded", service.reject(APPROVER, rejected.id, 0, "  Budget exceeded  ").rejectionReason)

        val approved = stored(status = RequestStatus.PENDING)
        assertNull(service.approve(APPROVER, approved.id, 0).rejectionReason)
    }

    @Test
    fun `second approve fails with state conflict at the new version and version conflict at the old one`() {
        val entity = stored(status = RequestStatus.PENDING, version = 0)
        service.approve(APPROVER, entity.id, 0)
        entity.version = 1 // JPA increments on flush; the mock repository does not.

        assertFailsWith<InvalidRequestTransition> { service.approve(APPROVER, entity.id, 1) }
        assertFailsWith<EquipmentRequestVersionConflict> { service.approve(APPROVER, entity.id, 0) }
        assertEquals(RequestStatus.APPROVED, entity.status)
        verify(exactly = 1) { repository.saveAndFlush(entity) }
    }

    @Test
    fun `missing request is not found for every action`() {
        val id = UUID.randomUUID()
        every { repository.findAggregateById(id) } returns null

        RequestAction.entries.forEach { action ->
            assertFailsWith<EquipmentRequestNotFound> {
                when (action) {
                    RequestAction.SUBMIT -> service.submit(OWNER, id, 0)
                    RequestAction.CANCEL -> service.cancel(OWNER, id, 0)
                    RequestAction.APPROVE -> service.approve(APPROVER, id, 0)
                    RequestAction.REJECT -> service.reject(APPROVER, id, 0, "reason")
                }
            }
        }
    }

    private fun perform(
        action: RequestAction,
        entity: EquipmentRequestEntity,
        actor: Actor,
        expectedVersion: Long,
        reason: String? = "Not in this quarter's budget",
    ): EquipmentRequestView = when (action) {
        RequestAction.SUBMIT -> service.submit(actor, entity.id, expectedVersion)
        RequestAction.CANCEL -> service.cancel(actor, entity.id, expectedVersion)
        RequestAction.APPROVE -> service.approve(actor, entity.id, expectedVersion)
        RequestAction.REJECT -> service.reject(actor, entity.id, expectedVersion, reason)
    }

    private fun stored(
        status: RequestStatus,
        version: Long = 0,
        items: List<NewEquipmentItem> = listOf(NewEquipmentItem(EquipmentType.NOTEBOOK, 1, null)),
        requiredDate: LocalDate = LocalDate.of(2026, 10, 15),
    ) = EquipmentRequestEntity(
        id = UUID.randomUUID(),
        requestNumber = "REQ-2026-000001",
        ownerId = OWNER.userId,
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Software Engineering",
        title = "Notebook for new project",
        purpose = "Develop the customer onboarding application",
        requiredDate = requiredDate,
        status = status,
        version = version,
        createdAt = STORED_AT,
        updatedAt = STORED_AT,
    ).apply {
        replaceItems(items)
        every { repository.findAggregateById(id) } returns this
        every { repository.saveAndFlush(this@apply) } answers { firstArg() }
    }

    companion object {
        private val OWNER = Actor("employee-001", ActorRole.EMPLOYEE)
        private val OTHER_EMPLOYEE = Actor("employee-002", ActorRole.EMPLOYEE)
        private val APPROVER = Actor("approver-001", ActorRole.APPROVER)
        private val STORED_AT = Instant.parse("2026-09-24T03:00:00Z")
        private val YESTERDAY = LocalDate.of(2026, 9, 24)

        private val ALLOWED = mapOf(
            (RequestStatus.DRAFT to RequestAction.SUBMIT) to RequestStatus.PENDING,
            (RequestStatus.DRAFT to RequestAction.CANCEL) to RequestStatus.CANCELLED,
            (RequestStatus.PENDING to RequestAction.CANCEL) to RequestStatus.CANCELLED,
            (RequestStatus.PENDING to RequestAction.APPROVE) to RequestStatus.APPROVED,
            (RequestStatus.PENDING to RequestAction.REJECT) to RequestStatus.REJECTED,
        )

        private fun actorFor(action: RequestAction) = when (action) {
            RequestAction.SUBMIT, RequestAction.CANCEL -> OWNER
            RequestAction.APPROVE, RequestAction.REJECT -> APPROVER
        }

        @JvmStatic
        fun statusActionMatrix() = RequestStatus.entries.flatMap { status ->
            RequestAction.entries.map { action -> Arguments.of(status, action) }
        }

        @JvmStatic
        fun actionActorMatrix() = RequestAction.entries.flatMap { action ->
            listOf(OWNER, OTHER_EMPLOYEE, APPROVER).map { actor -> Arguments.of(action, actor) }
        }

        @JvmStatic
        fun actions() = RequestAction.entries.map { Arguments.of(it) }
    }
}
