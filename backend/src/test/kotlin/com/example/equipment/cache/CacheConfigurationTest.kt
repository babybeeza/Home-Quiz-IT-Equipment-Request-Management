package com.example.equipment.cache

import com.example.equipment.application.NoOpRequestDetailCache
import com.example.equipment.application.RequestDetailCache
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.support.NoOpCacheManager
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class CacheConfigurationTest {
    @Test
    fun `switches off both caches without needing redis`() {
        ApplicationContextRunner()
            .withUserConfiguration(CacheConfiguration::class.java)
            .withPropertyValues("app.cache.request-detail.enabled=false", "app.cache.reference-data.enabled=false")
            .run { context ->
                assertSame(NoOpRequestDetailCache, context.getBean(RequestDetailCache::class.java))
                assertIs<NoOpCacheManager>(context.getBean(CacheManager::class.java))
            }
    }

    @Test
    fun `partial overrides keep each cache's own default TTL`() {
        ApplicationContextRunner()
            .withUserConfiguration(CacheConfiguration::class.java)
            .withPropertyValues("app.cache.request-detail.enabled=false")
            .run { context ->
                val properties = context.getBean(CacheProperties::class.java)
                assertEquals(Duration.ofMinutes(10), properties.requestDetail.ttl)
                assertEquals(Duration.ofHours(1), properties.referenceData.ttl)
                assertIs<CaffeineCacheManager>(context.getBean(CacheManager::class.java))
            }
    }
}
