package xyz.attacktive.morrowlight.core

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class GardenTest {
	@TestFactory
	fun gameAndSaveScenarios() = Scenarios.all.map { (name, scenario) ->
		DynamicTest.dynamicTest(name) { scenario() }
	}
}
