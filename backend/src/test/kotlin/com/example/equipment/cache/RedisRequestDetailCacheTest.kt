package com.example.equipment.cache

import com.example.equipment.application.EquipmentItemView
import com.example.equipment.application.EquipmentRequestView
import com.example.equipment.domain.EquipmentType
import com.example.equipment.domain.RequestStatus
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RedisRequestDetailCacheTest {
    private val values = mockk<ValueOperations<String, String>>(relaxUnitFun = true)
    private val redis = mockk<StringRedisTemplate> {
        every { opsForValue() } returns values
        every { delete(any<String>()) } returns true
    }
    private val registry = SimpleMeterRegistry()
    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()
    private val cache = RedisRequestDetailCache(redis, jsonMapper, Duration.ofMinutes(10), registry)
    private val view = EquipmentRequestView(
        id = UUID.fromString("0e0bda22-525e-4291-bdb2-87444e1c5c60"),
        requestNumber = "REQ-2026-000001",
        employeeName = "Somchai Developer",
        employeeEmail = "somchai@example.com",
        department = "Software Engineering",
        title = "Notebook for new project",
        purpose = "Develop the customer onboarding application",
        requiredDate = LocalDate.of(2099, 10, 15),
        additionalNote = null,
        status = RequestStatus.PENDING,
        rejectionReason = null,
        version = 7,
        items = listOf(
            EquipmentItemView(UUID.randomUUID(), EquipmentType.MONITOR, 2, "27 inch"),
            EquipmentItemView(UUID.randomUUID(), EquipmentType.MOUSE, 1, null),
        ),
        totalItems = 3,
        createdAt = Instant.parse("2026-09-25T03:00:00Z"),
        updatedAt = Instant.parse("2026-09-25T04:00:00Z"),
    )

    @Test
    fun `keys carry the format version, id and committed version`() {
        assertEquals("equipment:v1:request-detail:${view.id}:7", RedisRequestDetailCache.key(view.id, 7))
    }

    @Test
    fun `put writes JSON with the TTL and get round-trips every field in order`() {
        var stored: String? = null
        every { values.set(RedisRequestDetailCache.key(view.id, 7), any(), Duration.ofMinutes(10)) } answers {
            stored = secondArg()
        }
        cache.put(view)
        every { values.get(RedisRequestDetailCache.key(view.id, 7)) } answers { stored }

        assertEquals(view, cache.get(view.id, 7))
        assertEquals(1.0, count("hit"))
    }

    @Test
    fun `absent key is a miss`() {
        every { values.get(any()) } returns null

        assertNull(cache.get(view.id, 7))
        assertEquals(1.0, count("miss"))
    }

    @Test
    fun `redis failures become misses or skipped writes and are counted`() {
        every { values.get(any()) } throws RedisConnectionFailureException("down")
        every { values.set(any(), any(), any<Duration>()) } throws RedisConnectionFailureException("down")
        every { redis.delete(any<String>()) } throws RedisConnectionFailureException("down")

        assertNull(cache.get(view.id, 7))
        cache.put(view)
        cache.evict(view.id, 6)

        assertEquals(3.0, count("error"))
    }

    @Test
    fun `corrupt payload is counted, deleted and treated as a miss`() {
        val key = RedisRequestDetailCache.key(view.id, 7)
        every { values.get(key) } returns "{not json"

        assertNull(cache.get(view.id, 7))
        verify(exactly = 1) { redis.delete(key) }
        assertEquals(1.0, count("error"))
    }

    private fun count(result: String) =
        registry.get("equipment.cache.request_detail").tag("result", result).counter().count()
}
