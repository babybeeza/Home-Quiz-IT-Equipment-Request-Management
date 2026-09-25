package com.example.equipment.api

import com.example.equipment.application.EquipmentRequestService
import com.example.equipment.configuration.ApplicationConfiguration
import com.example.equipment.domain.EquipmentRequestValidator
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestEntity
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.NewEquipmentItem
import com.example.equipment.persistence.RequestNumberAllocator
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test

@WebMvcTest(EquipmentRequestController::class, properties = ["app.cors.allowed-origin=http://localhost:3100"])
@Import(ApplicationConfiguration::class, EquipmentRequestControllerTest.ServiceBeans::class)
class EquipmentRequestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var repository: EquipmentRequestRepository

    @Autowired
    private lateinit var allocator: RequestNumberAllocator

    @TestConfiguration
    class ServiceBeans {
        @Bean
        fun equipmentRequestRepository(): EquipmentRequestRepository = mockk()

        @Bean
        fun requestNumberAllocator(): RequestNumberAllocator = mockk()

        @Bean
        fun equipmentRequestService(
            repository: EquipmentRequestRepository,
            allocator: RequestNumberAllocator,
            validator: EquipmentRequestValidator,
            clock: Clock,
            businessZone: ZoneId,
        ) = EquipmentRequestService(repository, allocator, validator, clock, businessZone)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, allocator)
    }

    @Test
    fun `create returns the persisted draft with identity-derived ownership`() {
        every { allocator.nextSequence() } returns 12
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        postRequest(validBody()).andExpect {
            status { isCreated() }
            jsonPath("$.requestNumber") { value(matchesPattern("REQ-\\d{4}-000012")) }
            jsonPath("$.status") { value("DRAFT") }
            jsonPath("$.version") { value(0) }
            jsonPath("$.items[0].equipmentType") { value("NOTEBOOK") }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.ownerId") { doesNotExist() }
        }
        verify { repository.saveAndFlush(match { it.ownerId == "employee-001" }) }
    }

    @Test
    fun `missing or unknown identity headers return the malformed envelope`() {
        mockMvc.post(BASE) {
            header("X-Role", "EMPLOYEE")
            contentType = MediaType.APPLICATION_JSON
            content = validBody()
        }.andExpectMalformed()

        postRequest(validBody(), role = "ADMIN").andExpectMalformed()
        postRequest(validBody(), userId = "   ").andExpectMalformed()
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `malformed JSON and malformed UUID return the malformed envelope`() {
        postRequest("{\"employeeName\":").andExpectMalformed()
        postRequest(validBody().replace("\"NOTEBOOK\"", "\"TABLET\"")).andExpectMalformed()
        mockMvc.get("$BASE/not-a-uuid") { identity() }.andExpectMalformed()
    }

    @Test
    fun `nested item errors use indexed field paths`() {
        postRequest(validBody(quantity = 9)).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.status") { value(400) }
            jsonPath("$.path") { value(BASE) }
            jsonPath("$.fieldErrors['items[0].quantity']") { exists() }
        }
    }

    @Test
    fun `approver cannot create`() {
        postRequest(validBody(), userId = "approver-001", role = "APPROVER").andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("ACCESS_DENIED") }
        }
    }

    @Test
    fun `unknown request returns not found`() {
        val id = UUID.randomUUID()
        every { repository.findAggregateById(id) } returns null

        mockMvc.get("$BASE/$id") { identity() }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("REQUEST_NOT_FOUND") }
            jsonPath("$.fieldErrors") { isEmpty() }
        }
    }

    @Test
    fun `update error precedence is ownership then version then state then validation`() {
        val entity = existingEntity(version = 2)
        every { repository.findAggregateById(entity.id) } returns entity
        val invalidStale = validBody(expectedVersion = 1, title = "x")

        putRequest(entity.id, invalidStale, userId = "employee-002").andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("ACCESS_DENIED") }
        }
        putRequest(entity.id, invalidStale).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("REQUEST_VERSION_CONFLICT") }
        }
        entity.status = RequestStatus.PENDING
        putRequest(entity.id, validBody(expectedVersion = 2, title = "x")).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("REQUEST_STATE_CONFLICT") }
        }
        entity.status = RequestStatus.DRAFT
        putRequest(entity.id, validBody(expectedVersion = 2, title = "x")).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.fieldErrors.title") { exists() }
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `database optimistic lock failure maps to version conflict`() {
        val entity = existingEntity(version = 2)
        every { repository.findAggregateById(entity.id) } returns entity
        every { repository.saveAndFlush(any()) } throws OptimisticLockingFailureException("row changed")

        putRequest(entity.id, validBody(expectedVersion = 2)).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("REQUEST_VERSION_CONFLICT") }
        }
    }

    @Test
    fun `unexpected failures return a sanitized internal error`() {
        val entity = existingEntity(version = 2)
        every { repository.findAggregateById(entity.id) } returns entity
        every { repository.saveAndFlush(any()) } throws IllegalStateException("constraint ck_secret_detail violated")

        putRequest(entity.id, validBody(expectedVersion = 2)).andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("INTERNAL_ERROR") }
            jsonPath("$.message") { value("An unexpected error occurred") }
        }
    }

    @Test
    fun `CORS preflight allows the configured frontend origin and identity headers`() {
        mockMvc.options(BASE) {
            header(HttpHeaders.ORIGIN, "http://localhost:3100")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-User-Id,X-Role,Content-Type")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3100") }
        }
    }

    private fun postRequest(body: String, userId: String = "employee-001", role: String = "EMPLOYEE") =
        mockMvc.post(BASE) {
            header("X-User-Id", userId)
            header("X-Role", role)
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    private fun putRequest(id: UUID, body: String, userId: String = "employee-001") =
        mockMvc.put("$BASE/$id") {
            header("X-User-Id", userId)
            header("X-Role", "EMPLOYEE")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    private fun MockHttpServletRequestDsl.identity() {
        header("X-User-Id", "employee-001")
        header("X-Role", "EMPLOYEE")
    }

    private fun ResultActionsDsl.andExpectMalformed() = andExpect {
        status { isBadRequest() }
        jsonPath("$.code") { value("MALFORMED_REQUEST") }
        jsonPath("$.message") { value("Request format is invalid") }
        jsonPath("$.timestamp") { exists() }
    }

    private fun validBody(quantity: Int = 2, expectedVersion: Long? = null, title: String = "Notebook for new project") =
        """
        {
          "employeeName": "Somchai Developer",
          "employeeEmail": "somchai@example.com",
          "department": "Software Engineering",
          "title": "$title",
          "purpose": "Develop the customer onboarding application",
          "requiredDate": "2099-10-15",
          "items": [{ "equipmentType": "NOTEBOOK", "quantity": $quantity }]
          ${expectedVersion?.let { ", \"expectedVersion\": $it" } ?: ""}
        }
        """.trimIndent()

    private fun existingEntity(version: Long) = EquipmentRequestEntity(
        id = UUID.randomUUID(),
        requestNumber = "REQ-2026-000001",
        ownerId = "employee-001",
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Software Engineering",
        title = "Notebook for new project",
        purpose = "Develop the customer onboarding application",
        requiredDate = LocalDate.of(2099, 10, 15),
        version = version,
        createdAt = Instant.parse("2026-09-24T03:00:00Z"),
        updatedAt = Instant.parse("2026-09-24T03:00:00Z"),
    ).apply {
        replaceItems(listOf(NewEquipmentItem(EquipmentType.NOTEBOOK, 2, null)))
    }

    private companion object {
        const val BASE = "/api/v1/equipment-requests"
    }
}
