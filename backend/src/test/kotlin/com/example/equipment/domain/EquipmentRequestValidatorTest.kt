package com.example.equipment.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquipmentRequestValidatorTest {
    private val zone = ZoneId.of("Asia/Bangkok")
    private val clock = Clock.fixed(Instant.parse("2026-09-25T03:00:00Z"), zone)
    private val validator = EquipmentRequestValidator(clock, zone)

    @Test
    fun `valid draft may have no items`() {
        assertTrue(validator.validateDraft(validDraft(items = emptyList())).isEmpty())
    }

    @Test
    fun `submit requires at least one item`() {
        assertEquals(
            "At least one equipment item is required before submit",
            validator.validateForSubmit(validDraft(items = emptyList()))["items"],
        )
    }

    @Test
    fun `today is valid and yesterday is rejected in business timezone`() {
        assertFalse(validator.validateDraft(validDraft(requiredDate = LocalDate.of(2026, 9, 25))).containsKey("requiredDate"))
        assertTrue(validator.validateDraft(validDraft(requiredDate = LocalDate.of(2026, 9, 24))).containsKey("requiredDate"))
    }

    @Test
    fun `reports scalar and nested item validation paths`() {
        val errors = validator.validateDraft(
            validDraft(
                employeeName = " ",
                employeeEmail = "invalid",
                title = "1234",
                purpose = "short",
                items = listOf(EquipmentItemDraft(EquipmentType.NOTEBOOK, 6, "x".repeat(251))),
            ),
        )

        listOf(
            "employeeName",
            "employeeEmail",
            "title",
            "purpose",
            "items[0].quantity",
            "items[0].specification",
        ).forEach { field -> assertTrue(errors.containsKey(field), "missing error for $field") }
    }

    @Test
    fun `accepts exact maximum lengths and rejects values beyond them`() {
        val validAtBoundaries = EquipmentRequestDraft(
            employeeName = "n".repeat(100),
            employeeEmail = "a@example.com",
            department = "d".repeat(100),
            title = "t".repeat(150),
            purpose = "p".repeat(500),
            requiredDate = LocalDate.of(2026, 9, 25),
            additionalNote = "a".repeat(500),
            items = listOf(EquipmentItemDraft(EquipmentType.OTHER, 5, "s".repeat(250))),
        )
        assertTrue(validator.validateForSubmit(validAtBoundaries).isEmpty())

        val overLimits = validAtBoundaries.copy(
            employeeName = "n".repeat(101),
            employeeEmail = "a".repeat(245) + "@example.com",
            department = "d".repeat(101),
            title = "t".repeat(151),
            purpose = "p".repeat(501),
            additionalNote = "a".repeat(501),
            items = listOf(EquipmentItemDraft(EquipmentType.OTHER, 0, "s".repeat(251))),
        )
        val errors = validator.validateForSubmit(overLimits)
        listOf(
            "employeeName",
            "employeeEmail",
            "department",
            "title",
            "purpose",
            "additionalNote",
            "items[0].quantity",
            "items[0].specification",
        ).forEach { field -> assertTrue(errors.containsKey(field), "missing error for $field") }
    }

    @Test
    fun `reject reason is trimmed required and limited`() {
        assertTrue(validator.validateRejectionReason("valid reason").isEmpty())
        assertTrue(validator.validateRejectionReason("   ").containsKey("reason"))
        assertTrue(validator.validateRejectionReason("x".repeat(501)).containsKey("reason"))
    }

    private fun validDraft(
        employeeName: String = "Somchai Developer",
        employeeEmail: String = "somchai@example.com",
        title: String = "Notebook for new project",
        purpose: String = "Develop the customer onboarding application",
        requiredDate: LocalDate = LocalDate.of(2026, 10, 15),
        items: List<EquipmentItemDraft> = listOf(EquipmentItemDraft(EquipmentType.NOTEBOOK, 1)),
    ) = EquipmentRequestDraft(
        employeeName = employeeName,
        employeeEmail = employeeEmail,
        department = "Software Engineering",
        title = title,
        purpose = purpose,
        requiredDate = requiredDate,
        items = items,
    )
}
