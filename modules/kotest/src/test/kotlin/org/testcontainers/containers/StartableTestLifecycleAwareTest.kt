package org.testcontainers.containers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize

class StartableTestLifecycleAwareTest : StringSpec({
    val startableTestLifecycleAwareForPerTest = StartableTestLifecycleAware()
    val startableTestLifecycleAwareForPerSpec = StartableTestLifecycleAware()

    listeners(startableTestLifecycleAwareForPerTest.perTest(), startableTestLifecycleAwareForPerSpec.perSpec())

    "beforeTestCount for first test should be one" {
        startableTestLifecycleAwareForPerTest.testDescriptions shouldHaveSize 1
        startableTestLifecycleAwareForPerSpec.testDescriptions shouldHaveSize 1
    }

    "beforeTestCount for second test should be two" {
        startableTestLifecycleAwareForPerTest.testDescriptions shouldHaveSize 2
        startableTestLifecycleAwareForPerTest.testDescriptions shouldHaveSize 2
    }
})
