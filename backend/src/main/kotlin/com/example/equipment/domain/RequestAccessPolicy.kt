package com.example.equipment.domain

enum class ActorRole {
    EMPLOYEE,
    APPROVER,
}

data class Actor(
    val userId: String,
    val role: ActorRole,
)

enum class RequestOperation {
    CREATE,
    VIEW,
    EDIT,
    SUBMIT,
    CANCEL,
    APPROVE,
    REJECT,
}

object RequestAccessPolicy {
    fun isAllowed(actor: Actor, operation: RequestOperation, ownerId: String? = null): Boolean =
        when (operation) {
            RequestOperation.CREATE -> actor.role == ActorRole.EMPLOYEE
            RequestOperation.VIEW -> actor.role == ActorRole.APPROVER || isOwnerEmployee(actor, ownerId)
            RequestOperation.EDIT,
            RequestOperation.SUBMIT,
            RequestOperation.CANCEL,
            -> isOwnerEmployee(actor, ownerId)
            RequestOperation.APPROVE,
            RequestOperation.REJECT,
            -> actor.role == ActorRole.APPROVER
        }

    private fun isOwnerEmployee(actor: Actor, ownerId: String?): Boolean =
        actor.role == ActorRole.EMPLOYEE && ownerId != null && actor.userId == ownerId
}
