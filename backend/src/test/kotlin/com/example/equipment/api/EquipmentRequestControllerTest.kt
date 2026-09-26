package com.example.equipment.api

import com.example.equipment.application.EquipmentRequestQueryService
import com.example.equipment.application.EquipmentRequestService
import com.example.equipment.application.NoOpRequestDetailCache
import com.example.equipment.configuration.ApplicationConfiguration
import com.example.equipment.domain.EquipmentRequestValidator
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import com.example.equipment.persistence.EquipmentRequestEntity
import com.example.equipment.persistence.EquipmentRequestRepository
import com.example.equipment.persistence.ItemQuantityTotal
import com.example.equipment.persistence.NewEquipmentItem
import com.example.equipment.persistence.RequestNumberAllocator
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
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
import kotlin.test.assertEquals

@WebMvcTest(EquipmentRequestController::class, properties = ["app.cors.allowed-origin=http://localhost:3100, http://frontend:3000"])
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
        ) = EquipmentRequestService(repository, allocator, validator, clock, businessZone, NoOpRequestDetailCache)

        @Bean
        fun equipmentRequestQueryService(repository: EquipmentRequestRepository) =
            EquipmentRequestQueryService(repository)
    }

    @Test
    fun `list applies contract defaults and returns page metadata`() {
        val entity = existingEntity(version = 1)
        val pageable = slot<Pageable>()
        every { repository.findAll(any<Specification<EquipmentRequestEntity>>(), capture(pageable)) } answers {
            PageImpl(listOf(entity), pageable.captured, 11)
        }
        every { repository.sumItemQuantities(listOf(entity.id)) } returns listOf(ItemQuantityTotal(entity.id, 2))

        mockMvc.get(BASE) { identity() }.andExpect {
            status { isOk() }
            jsonPath("$.content[0].requestNumber") { value("REQ-2026-000001") }
            jsonPath("$.content[0].totalItems") { value(2) }
            jsonPath("$.content[0].items") { doesNotExist() }
            jsonPath("$.page") { value(0) }
            jsonPath("$.size") { value(10) }
            jsonPath("$.totalElements") { value(11) }
            jsonPath("$.totalPages") { value(2) }
        }
        assertEquals(0, pageable.captured.pageNumber)
        assertEquals(10, pageable.captured.pageSize)
        assertEquals(
            Sort.by(Sort.Direction.DESC, "createdAt", "id"),
            pageable.captured.sort,
        )
    }

    @Test
    fun `list sort ascending orders by createdAt then id ascending`() {
        val pageable = slot<Pageable>()
        every { repository.findAll(any<Specification<EquipmentRequestEntity>>(), capture(pageable)) } answers {
            PageImpl(emptyList(), pageable.captured, 0)
        }

        mockMvc.get("$BASE?sort=createdAt,asc&page=3&size=25") { identity() }.andExpect {
            status { isOk() }
            jsonPath("$.content") { isEmpty() }
            jsonPath("$.page") { value(3) }
            jsonPath("$.totalPages") { value(0) }
        }
        assertEquals(Sort.by(Sort.Direction.ASC, "createdAt", "id"), pageable.captured.sort)
        assertEquals(3, pageable.captured.pageNumber)
        verify(exactly = 0) { repository.sumItemQuantities(any()) }
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource(
        "page, -1", "page, x", "size, 0", "size, 101", "size, x",
        "sort, 'title,asc'", "status, OPEN", "status, pending",
    )
    fun `invalid list parameters name the failing field`(field: String, value: String) {
        mockMvc.get(BASE) {
            identity()
            param(field, value)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_ERROR") }
            jsonPath("$.fieldErrors.$field") { exists() }
        }
        verify(exactly = 0) { repository.findAll(any<Specification<EquipmentRequestEntity>>(), any<Pageable>()) }
    }

    @Test
    fun `overlong keyword and department are validation errors and identity is checked first`() {
        mockMvc.get(BASE) {
            identity()
            param("keyword", "k".repeat(151))
            param("department", "d".repeat(101))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors.keyword") { exists() }
            jsonPath("$.fieldErrors.department") { exists() }
        }
        mockMvc.get("$BASE?size=0") { header("X-Role", "EMPLOYEE") }.andExpectMalformed()
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
        every { repository.findAccessHeaderById(id) } returns null

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
    fun `an unmatched API path returns not found in the shared envelope`() {
        mockMvc.get("/api/v1/nonexistent-resource") { identity() }.andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.code") { value("NOT_FOUND") }
            jsonPath("$.path") { value("/api/v1/nonexistent-resource") }
            jsonPath("$.fieldErrors") { isEmpty() }
        }
    }

    @Test
    fun `an unsupported method returns method not allowed with the Allow header`() {
        mockMvc.delete("$BASE/${UUID.randomUUID()}") { identity() }.andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.code") { value("METHOD_NOT_ALLOWED") }
            header { string(HttpHeaders.ALLOW, matchesPattern(".*GET.*")) }
            header { string(HttpHeaders.ALLOW, matchesPattern(".*PUT.*")) }
        }
    }

    @Test
    fun `an unsupported content type returns unsupported media type and creates nothing`() {
        mockMvc.post(BASE) {
            identity()
            contentType = MediaType.TEXT_PLAIN
            content = validBody()
        }.andExpect {
            status { isUnsupportedMediaType() }
            jsonPath("$.code") { value("UNSUPPORTED_MEDIA_TYPE") }
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `an unacceptable response format returns 406 without a body`() {
        every { repository.findAll(any<Specification<EquipmentRequestEntity>>(), any<Pageable>()) } returns
            PageImpl(emptyList(), Pageable.ofSize(10), 0)

        mockMvc.get(BASE) {
            identity()
            accept = MediaType.valueOf("text/csv")
        }.andExpect {
            status { isNotAcceptable() }
            content { string("") }
        }
    }

    @Test
    fun `a client that accepts no JSON is rejected before create or a transition runs`() {
        val entity = existingEntity(version = 0)
        every { repository.findAggregateById(entity.id) } returns entity

        mockMvc.post(BASE) {
            identity()
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.valueOf("text/csv")
            content = validBody()
        }.andExpect { status { isNotAcceptable() } }
        mockMvc.post("$BASE/${entity.id}/submit") {
            identity()
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.valueOf("text/csv")
            content = """{"expectedVersion":0}"""
        }.andExpect { status { isNotAcceptable() } }

        verify(exactly = 0) { allocator.nextSequence() }
        verify(exactly = 0) { repository.findAggregateById(any()) }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @ParameterizedTest(name = "Accept: {0}")
    @CsvSource("*/*", "application/json", "'application/json, text/csv;q=0.5'", "''")
    fun `JSON-compatible or absent Accept headers still reach the handler`(accept: String) {
        every { repository.findAll(any<Specification<EquipmentRequestEntity>>(), any<Pageable>()) } returns
            PageImpl(emptyList(), Pageable.ofSize(10), 0)

        mockMvc.get(BASE) {
            identity()
            if (accept.isNotEmpty()) header(HttpHeaders.ACCEPT, accept)
        }.andExpect {
            status { isOk() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
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

    @Test
    fun `every configured origin may mutate and an unlisted origin is rejected`() {
        val entity = existingEntity(version = 0)
        every { repository.findAggregateById(entity.id) } returns entity
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        // A same-origin POST proxied by the frontend still carries Origin (TASK-009 Docker stack).
        mockMvc.post("$BASE/${entity.id}/submit") {
            header(HttpHeaders.ORIGIN, "http://frontend:3000")
            header("X-User-Id", "employee-001")
            header("X-Role", "EMPLOYEE")
            contentType = MediaType.APPLICATION_JSON
            content = """{"expectedVersion":0}"""
        }.andExpect { status { isOk() } }

        mockMvc.post("$BASE/${entity.id}/submit") {
            header(HttpHeaders.ORIGIN, "http://evil.example")
            header("X-User-Id", "employee-001")
            header("X-Role", "EMPLOYEE")
            contentType = MediaType.APPLICATION_JSON
            content = """{"expectedVersion":1}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `submit returns the updated request as PENDING`() {
        val entity = existingEntity(version = 2)
        every { repository.findAggregateById(entity.id) } returns entity
        every { repository.saveAndFlush(any()) } answers { firstArg() }

        action(entity.id, "submit", """{"expectedVersion":2}""").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.id") { value(entity.id.toString()) }
        }
    }

    @Test
    fun `action bodies that are missing or malformed return the malformed envelope`() {
        val entity = existingEntity(version = 0)
        every { repository.findAggregateById(entity.id) } returns entity

        listOf("submit", "cancel", "approve", "reject").forEach { name ->
            val (userId, role) = if (name in listOf("approve", "reject")) "approver-001" to "APPROVER" else "employee-001" to "EMPLOYEE"
            mockMvc.post("$BASE/${entity.id}/$name") {
                header("X-User-Id", userId)
                header("X-Role", role)
                contentType = MediaType.APPLICATION_JSON
            }.andExpectMalformed()
            action(entity.id, name, "not json", userId, role).andExpectMalformed()
            action(entity.id, name, """{"expectedVersion":"one"}""", userId, role).andExpectMalformed()
            action(entity.id, name, "{}", userId, role).andExpectMalformed()
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `business rule failures use 422 with a stable code and field errors`() {
        val empty = existingEntity(version = 0).apply { replaceItems(emptyList()) }
        every { repository.findAggregateById(empty.id) } returns empty
        action(empty.id, "submit", """{"expectedVersion":0}""").andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.code") { value("ITEMS_REQUIRED") }
            jsonPath("$.fieldErrors.items") { exists() }
        }

        val pending = existingEntity(version = 0).apply { status = RequestStatus.PENDING }
        every { repository.findAggregateById(pending.id) } returns pending
        listOf("""{"expectedVersion":0}""", """{"expectedVersion":0,"reason":"   "}""").forEach { body ->
            action(pending.id, "reject", body, "approver-001", "APPROVER").andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.code") { value("REJECTION_REASON_REQUIRED") }
                jsonPath("$.fieldErrors.reason") { exists() }
            }
        }
        verify(exactly = 0) { repository.saveAndFlush(any()) }
    }

    @Test
    fun `invalid transitions, unknown requests and flush conflicts use the shared envelope`() {
        val approved = existingEntity(version = 1).apply { status = RequestStatus.APPROVED }
        every { repository.findAggregateById(approved.id) } returns approved
        action(approved.id, "approve", """{"expectedVersion":1}""", "approver-001", "APPROVER").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("REQUEST_STATE_CONFLICT") }
        }

        val missing = UUID.randomUUID()
        every { repository.findAggregateById(missing) } returns null
        action(missing, "cancel", """{"expectedVersion":0}""").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("REQUEST_NOT_FOUND") }
        }

        val pending = existingEntity(version = 0).apply { status = RequestStatus.PENDING }
        every { repository.findAggregateById(pending.id) } returns pending
        every { repository.saveAndFlush(any()) } throws OptimisticLockingFailureException("row changed")
        action(pending.id, "approve", """{"expectedVersion":0}""", "approver-001", "APPROVER").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("REQUEST_VERSION_CONFLICT") }
        }
    }

    private fun action(
        id: UUID,
        name: String,
        body: String,
        userId: String = "employee-001",
        role: String = "EMPLOYEE",
    ) = mockMvc.post("$BASE/$id/$name") {
        header("X-User-Id", userId)
        header("X-Role", role)
        contentType = MediaType.APPLICATION_JSON
        content = body
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
