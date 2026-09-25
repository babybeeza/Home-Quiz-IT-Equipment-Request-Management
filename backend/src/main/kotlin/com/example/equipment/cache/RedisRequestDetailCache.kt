package com.example.equipment.cache

import com.example.equipment.application.EquipmentRequestView
import com.example.equipment.application.RequestDetailCache
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.util.UUID

/**
 * Shared request-detail cache (ADR-006). Keys embed the committed version, so an entry can never be
 * served for another version. Only Redis/serialization failures are absorbed here; callers read
 * PostgreSQL on a miss, and database errors are never caught by this class.
 */
class RedisRequestDetailCache(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
    private val ttl: Duration,
    registry: MeterRegistry,
) : RequestDetailCache {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val hits = counter(registry, "hit")
    private val misses = counter(registry, "miss")
    private val errors = counter(registry, "error")

    override fun get(id: UUID, version: Long): EquipmentRequestView? {
        val key = key(id, version)
        val payload = try {
            redis.opsForValue().get(key)
        } catch (failure: DataAccessException) {
            recordError("read", id, failure)
            return null
        }
        if (payload == null) {
            misses.increment()
            return null
        }
        return try {
            jsonMapper.readValue(payload, EquipmentRequestView::class.java).also { hits.increment() }
        } catch (failure: JacksonException) {
            recordError("deserialize", id, failure)
            evictKey(key, id)
            null
        }
    }

    override fun put(view: EquipmentRequestView) {
        try {
            redis.opsForValue().set(key(view.id, view.version), jsonMapper.writeValueAsString(view), ttl)
        } catch (failure: DataAccessException) {
            recordError("write", view.id, failure)
        }
    }

    override fun evict(id: UUID, version: Long) = evictKey(key(id, version), id)

    private fun evictKey(key: String, id: UUID) {
        try {
            redis.delete(key)
        } catch (failure: DataAccessException) {
            recordError("evict", id, failure)
        }
    }

    private fun recordError(operation: String, id: UUID, failure: Exception) {
        errors.increment()
        // Request id and exception type only: payloads contain names and emails.
        logger.warn("Request detail cache {} failed for request {}: {}", operation, id, failure.javaClass.simpleName)
    }

    companion object {
        const val KEY_PREFIX = "equipment:v1:request-detail:"

        fun key(id: UUID, version: Long) = "$KEY_PREFIX$id:$version"

        private fun counter(registry: MeterRegistry, result: String) =
            Counter.builder("equipment.cache.request_detail")
                .description("Redis request-detail cache lookups and failures")
                .tag("result", result)
                .register(registry)
    }
}
