package com.example.equipment.cache

import com.example.equipment.application.REFERENCE_DATA_CACHE
import com.example.equipment.application.ReferenceDataService
import com.example.equipment.persistence.DepartmentEntity
import com.example.equipment.persistence.DepartmentRepository
import com.github.benmanes.caffeine.cache.Ticker
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/** Load → hit → expiry for the local Caffeine cache through the real @Cacheable proxy and a controllable clock. */
@SpringJUnitConfig(ReferenceDataCacheTest.Config::class)
class ReferenceDataCacheTest {
    @Autowired
    private lateinit var service: ReferenceDataService

    @Autowired
    private lateinit var departments: DepartmentRepository

    @Autowired
    private lateinit var ticker: FakeTicker

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun reset() {
        cacheManager.getCache(REFERENCE_DATA_CACHE)!!.clear()
        clearMocks(departments)
        every { departments.findByActiveTrueOrderBySortOrderAsc() } returns listOf(
            DepartmentEntity("FINANCE", "Finance", 20),
            DepartmentEntity("DESIGN", "Design", 60),
        )
    }

    @Test
    fun `second call is served from Caffeine without touching the database`() {
        val first = service.get()
        val second = service.get()

        assertSame(first, second)
        assertEquals(listOf("Finance", "Design"), first.departments.map { it.name })
        assertEquals(6, first.equipmentTypes.size)
        verify(exactly = 1) { departments.findByActiveTrueOrderBySortOrderAsc() }
        val stats = (cacheManager.getCache(REFERENCE_DATA_CACHE) as CaffeineCache).nativeCache.stats()
        assertEquals(1, stats.hitCount())
        assertEquals(1, stats.missCount())
    }

    @Test
    fun `entry expires after the configured TTL and is reloaded`() {
        service.get()
        ticker.advance(Duration.ofMinutes(59))
        service.get()
        verify(exactly = 1) { departments.findByActiveTrueOrderBySortOrderAsc() }

        ticker.advance(Duration.ofMinutes(2))
        service.get()
        verify(exactly = 2) { departments.findByActiveTrueOrderBySortOrderAsc() }
    }

    @Test
    fun `disabled switch uses a no-op cache manager`() {
        assertIs<NoOpCacheManager>(referenceDataCacheManager(false, Duration.ofHours(1), Ticker.systemTicker()))
    }

    class FakeTicker : Ticker {
        private val nanos = AtomicLong()

        override fun read(): Long = nanos.get()

        fun advance(duration: Duration) {
            nanos.addAndGet(duration.toNanos())
        }
    }

    @Configuration
    @EnableCaching
    class Config {
        @Bean
        fun fakeTicker() = FakeTicker()

        @Bean
        fun cacheManager(ticker: FakeTicker): CacheManager = referenceDataCacheManager(true, Duration.ofHours(1), ticker)

        @Bean
        fun departmentRepository(): DepartmentRepository = mockk()

        @Bean
        fun referenceDataService(departments: DepartmentRepository) = ReferenceDataService(departments)
    }
}
