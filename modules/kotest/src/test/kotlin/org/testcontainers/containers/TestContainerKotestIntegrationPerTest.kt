package org.testcontainers.containers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TestContainerKotestIntegrationPerTest : StringSpec({
    val testStartable = TestStartable()
    listeners(testStartable.perSpec())

    "start count for first test should be one" {
        testStartable.startCount shouldBe 1
    }

    "start count for second test should be two" {
        testStartable.startCount shouldBe 2
    }
})
