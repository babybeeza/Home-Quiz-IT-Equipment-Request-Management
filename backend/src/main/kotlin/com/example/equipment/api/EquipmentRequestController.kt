package com.example.equipment.api

import com.example.equipment.application.EquipmentRequestQueryService
import com.example.equipment.application.EquipmentRequestService
import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/equipment-requests")
class EquipmentRequestController(
    private val service: EquipmentRequestService,
    private val queryService: EquipmentRequestQueryService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        @Valid @RequestBody body: CreateEquipmentRequestBody,
    ): EquipmentRequestResponse = service.create(actor(userId, role), body.toDraft()).toResponse()

    @GetMapping
    fun search(
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) page: String?,
        @RequestParam(required = false) size: String?,
        @RequestParam(required = false) sort: String?,
    ): EquipmentRequestPageResponse {
        val actor = actor(userId, role) // identity errors take precedence over parameter errors
        val criteria = parseSearchCriteria(keyword, status, department, page, size, sort)
        return queryService.search(actor, criteria).toResponse()
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
    ): EquipmentRequestResponse = service.get(actor(userId, role), id).toResponse()

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        // No @Valid: the service validates after ownership, version and state checks to keep the contract's error precedence.
        @RequestBody body: UpdateEquipmentRequestBody,
    ): EquipmentRequestResponse =
        service.update(actor(userId, role), id, body.toDraft(), body.expectedVersion).toResponse()

    // Action bodies are not @Valid for the same precedence reason as update.
    @PostMapping("/{id}/submit")
    fun submit(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        @RequestBody body: VersionActionBody,
    ): EquipmentRequestResponse = service.submit(actor(userId, role), id, body.expectedVersion).toResponse()

    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        @RequestBody body: VersionActionBody,
    ): EquipmentRequestResponse = service.cancel(actor(userId, role), id, body.expectedVersion).toResponse()

    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        @RequestBody body: VersionActionBody,
    ): EquipmentRequestResponse = service.approve(actor(userId, role), id, body.expectedVersion).toResponse()

    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-Role") role: String,
        @RequestBody body: RejectActionBody,
    ): EquipmentRequestResponse =
        service.reject(actor(userId, role), id, body.expectedVersion, body.reason).toResponse()

    private fun actor(userId: String, rawRole: String): Actor {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isEmpty() || normalizedUserId.length > 100) throw MalformedIdentity()
        val role = try {
            ActorRole.valueOf(rawRole)
        } catch (_: IllegalArgumentException) {
            throw MalformedIdentity()
        }
        return Actor(normalizedUserId, role)
    }
}

class MalformedIdentity : RuntimeException("X-User-Id or X-Role is invalid")

