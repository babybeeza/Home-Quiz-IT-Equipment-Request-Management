package com.example.equipment.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestStatusTest {
    @Test
    fun `allows every specified transition`() {
        val transitions = mapOf(
            RequestStatus.DRAFT to mapOf(
                RequestAction.SUBMIT to RequestStatus.PENDING,
                RequestAction.CANCEL to RequestStatus.CANCELLED,
            ),
            RequestStatus.PENDING to mapOf(
                RequestAction.APPROVE to RequestStatus.APPROVED,
                RequestAction.REJECT to RequestStatus.REJECTED,
                RequestAction.CANCEL to RequestStatus.CANCELLED,
            ),
        )

        transitions.forEach { (from, actions) ->
            actions.forEach { (action, expected) ->
                assertEquals(expected, from.transition(action))
            }
        }
    }

    @Test
    fun `rejects transitions from terminal states`() {
        listOf(RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED).forEach { status ->
            RequestAction.entries.forEach { action ->
                assertFailsWith<InvalidRequestTransition> { status.transition(action) }
            }
        }
    }

    @Test
    fun `rejects unspecified transitions from active states`() {
        listOf(
            RequestStatus.DRAFT to RequestAction.APPROVE,
            RequestStatus.DRAFT to RequestAction.REJECT,
            RequestStatus.PENDING to RequestAction.SUBMIT,
        ).forEach { (status, action) ->
            assertFailsWith<InvalidRequestTransition> { status.transition(action) }
        }
    }

    @Test
    fun `only draft is editable`() {
        RequestStatus.DRAFT.requireEditable()
        RequestStatus.entries.filterNot { it == RequestStatus.DRAFT }.forEach { status ->
            assertFailsWith<RequestNotEditable> { status.requireEditable() }
        }
    }
}
