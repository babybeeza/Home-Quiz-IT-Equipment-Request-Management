package com.example.equipment.api

import com.example.equipment.domain.Actor
import com.example.equipment.domain.ActorRole

/** Demo identity from X-User-Id / X-Role headers; not authentication (ADR-003). */
fun parseActor(userId: String, rawRole: String): Actor {
    val normalizedUserId = userId.trim()
    if (normalizedUserId.isEmpty() || normalizedUserId.length > 100) throw MalformedIdentity()
    val role = try {
        ActorRole.valueOf(rawRole)
    } catch (_: IllegalArgumentException) {
        throw MalformedIdentity()
    }
    return Actor(normalizedUserId, role)
}

class MalformedIdentity : RuntimeException("X-User-Id or X-Role is invalid")
