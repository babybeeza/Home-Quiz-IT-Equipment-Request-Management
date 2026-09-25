package com.example.equipment.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestAccessPolicyTest {
    private val owner = Actor("employee-001", ActorRole.EMPLOYEE)
    private val otherEmployee = Actor("employee-002", ActorRole.EMPLOYEE)
    private val approver = Actor("approver-001", ActorRole.APPROVER)

    @Test
    fun `employee can create and operate only on owned requests`() {
        assertTrue(RequestAccessPolicy.isAllowed(owner, RequestOperation.CREATE))
        listOf(RequestOperation.VIEW, RequestOperation.EDIT, RequestOperation.SUBMIT, RequestOperation.CANCEL)
            .forEach { operation ->
                assertTrue(RequestAccessPolicy.isAllowed(owner, operation, owner.userId))
                assertFalse(RequestAccessPolicy.isAllowed(otherEmployee, operation, owner.userId))
            }
    }

    @Test
    fun `approver can view approve and reject but cannot create edit submit or cancel`() {
        listOf(RequestOperation.VIEW, RequestOperation.APPROVE, RequestOperation.REJECT).forEach { operation ->
            assertTrue(RequestAccessPolicy.isAllowed(approver, operation, owner.userId))
        }
        listOf(RequestOperation.CREATE, RequestOperation.EDIT, RequestOperation.SUBMIT, RequestOperation.CANCEL)
            .forEach { operation ->
                assertFalse(RequestAccessPolicy.isAllowed(approver, operation, owner.userId))
            }
    }
}
