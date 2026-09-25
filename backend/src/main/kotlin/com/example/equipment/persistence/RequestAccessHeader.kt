package com.example.equipment.persistence

import java.util.UUID

/** The authoritative owner and version read by primary key before any cached detail is used (ADR-006). */
data class RequestAccessHeader(val id: UUID, val ownerId: String, val version: Long)
