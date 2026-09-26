package com.example.equipment.api

import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiExceptionHandlerTest {
    private val handler = ApiExceptionHandler(Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneOffset.UTC))
    private val request = MockHttpServletRequest("GET", "/api/v1/equipment-requests")

    @Test
    fun `an unmapped framework client error keeps its status with the malformed code`() {
        val response = handler.unexpected(MissingServletRequestParameterException("page", "int"), request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("MALFORMED_REQUEST", response.body?.code)
        assertEquals("/api/v1/equipment-requests", response.body?.path)
    }

    @Test
    fun `a framework server error stays a sanitized internal error`() {
        val response = handler.unexpected(ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "pool exhausted"), request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_ERROR", response.body?.code)
        assertEquals("An unexpected error occurred", response.body?.message)
    }
}
