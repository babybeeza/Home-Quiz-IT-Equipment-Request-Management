package com.example.equipment.configuration

import com.example.equipment.domain.EquipmentRequestValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Clock
import java.time.ZoneId

@Configuration
class ApplicationConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun businessZone(): ZoneId = ZoneId.of("Asia/Bangkok")

    @Bean
    fun equipmentRequestValidator(clock: Clock, businessZone: ZoneId) =
        EquipmentRequestValidator(clock, businessZone)

    @Bean
    fun corsConfiguration(
        @Value("\${app.cors.allowed-origin}") allowedOrigin: String,
    ): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", "X-User-Id", "X-Role")
        }
    }
}

