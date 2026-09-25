package com.example.equipment

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class EquipmentRequestServiceApplicationTests {

	@Test
	fun `JUnit and MockK execute through the Maven test runner`() {
		val dependency = mockk<FoundationDependency>()
		every { dependency.status() } returns "ready"

		assertEquals("ready", dependency.status())
	}
}

private interface FoundationDependency {
	fun status(): String
}
