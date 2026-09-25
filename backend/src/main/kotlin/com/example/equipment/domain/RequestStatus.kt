package com.example.equipment.domain

enum class RequestStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    fun transition(action: RequestAction): RequestStatus = when (this to action) {
        DRAFT to RequestAction.SUBMIT -> PENDING
        DRAFT to RequestAction.CANCEL -> CANCELLED
        PENDING to RequestAction.APPROVE -> APPROVED
        PENDING to RequestAction.REJECT -> REJECTED
        PENDING to RequestAction.CANCEL -> CANCELLED
        else -> throw InvalidRequestTransition(this, action)
    }

    fun requireEditable() {
        if (this != DRAFT) {
            throw RequestNotEditable(this)
        }
    }
}

enum class RequestAction {
    SUBMIT,
    APPROVE,
    REJECT,
    CANCEL,
}

class InvalidRequestTransition(
    val currentStatus: RequestStatus,
    val action: RequestAction,
) : IllegalStateException("Cannot $action an equipment request in $currentStatus status")

class RequestNotEditable(
    val currentStatus: RequestStatus,
) : IllegalStateException("Equipment request in $currentStatus status is not editable")
