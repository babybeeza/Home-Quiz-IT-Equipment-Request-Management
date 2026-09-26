package com.example.equipment.api

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals

/** TASK-017 / R3: a client that accepts no JSON must get 406 before anything is committed. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AcceptBeforeMutationIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun emptyTables() {
        jdbc.execute("truncate table equipment_requests cascade")
    }

    @Test
    fun `create with a non-JSON Accept commits nothing and consumes no request number`() {
        val sequenceBefore = sequenceValue()

        mockMvc.post(BASE) {
            employee()
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.valueOf("text/csv")
            content = BODY
        }.andExpect {
            status { isNotAcceptable() }
            content { string("") }
        }

        assertEquals(0, count())
        assertEquals(sequenceBefore, sequenceValue())
    }

    @Test
    fun `a transition with a non-JSON Accept leaves status and version unchanged`() {
        val id = mockMvc.post(BASE) {
            employee()
            contentType = MediaType.APPLICATION_JSON
            content = BODY
        }.andExpect { status { isCreated() } }
            .andReturn().response.contentAsString
            .let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }

        mockMvc.post("$BASE/$id/submit") {
            employee()
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.valueOf("text/csv")
            content = """{"expectedVersion":0}"""
        }.andExpect { status { isNotAcceptable() } }

        assertEquals("DRAFT v0", jdbc.queryForObject("select status || ' v' || version from equipment_requests where id = ?::uuid", String::class.java, id))
    }

    private fun MockHttpServletRequestDsl.employee() {
        header("X-User-Id", "employee-001")
        header("X-Role", "EMPLOYEE")
    }

    private fun count() = jdbc.queryForObject("select count(*) from equipment_requests", Int::class.java)

    private fun sequenceValue() = jdbc.queryForObject(
        "select case when is_called then last_value else 0 end from equipment_request_number_seq",
        Long::class.java,
    )

    companion object {
        const val BASE = "/api/v1/equipment-requests"

        val BODY = """
            {"employeeName":"Somchai Developer","employeeEmail":"somchai@example.com","department":"Software Engineering",
             "title":"Notebook for new project","purpose":"Develop the customer onboarding application",
             "requiredDate":"2099-10-15","items":[{"equipmentType":"NOTEBOOK","quantity":1}]}
        """.trimIndent()

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
