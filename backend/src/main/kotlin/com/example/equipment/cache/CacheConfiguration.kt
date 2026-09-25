package com.example.equipment.cache

import com.example.equipment.application.NoOpRequestDetailCache
import com.example.equipment.application.REFERENCE_DATA_CACHE
import com.example.equipment.application.RequestDetailCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/** `app.cache.*` switches and TTLs (ADR-006); TASK-007 toggles them for enabled/disabled k6 runs. */
@ConfigurationProperties("app.cache")
data class CacheProperties(
    val requestDetail: RequestDetail = RequestDetail(),
    val referenceData: ReferenceData = ReferenceData(),
) {
    /** Each setting has its own default so a partial override (e.g. only `enabled`) still binds. */
    data class RequestDetail(val enabled: Boolean = true, val ttl: Duration = Duration.ofMinutes(10))

    data class ReferenceData(val enabled: Boolean = true, val ttl: Duration = Duration.ofHours(1))
}

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties::class)
class CacheConfiguration {
    @Bean
    @ConditionalOnProperty(name = ["app.cache.request-detail.enabled"], havingValue = "true", matchIfMissing = true)
    fun redisRequestDetailCache(
        redis: StringRedisTemplate,
        jsonMapper: JsonMapper,
        registry: MeterRegistry,
        properties: CacheProperties,
    ): RequestDetailCache = RedisRequestDetailCache(redis, jsonMapper, properties.requestDetail.ttl, registry)

    @Bean
    @ConditionalOnProperty(name = ["app.cache.request-detail.enabled"], havingValue = "false")
    fun disabledRequestDetailCache(): RequestDetailCache = NoOpRequestDetailCache

    @Bean
    fun cacheTicker(): Ticker = Ticker.systemTicker()

    /** The only Spring CacheManager: annotation caching is local Caffeine, never Redis (ADR-006). */
    @Bean
    fun cacheManager(ticker: Ticker, properties: CacheProperties): CacheManager =
        referenceDataCacheManager(properties.referenceData.enabled, properties.referenceData.ttl, ticker)
}

fun referenceDataCacheManager(enabled: Boolean, ttl: Duration, ticker: Ticker): CacheManager {
    if (!enabled) return NoOpCacheManager()
    return CaffeineCacheManager(REFERENCE_DATA_CACHE).apply {
        setCaffeine(Caffeine.newBuilder().maximumSize(16).expireAfterWrite(ttl).ticker(ticker).recordStats())
        isAllowNullValues = false
    }
}
