package org.testcontainers.containers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TestLifecycleAwareListenerTest : StringSpec({
    val startableTestLifecycleAware = StartableTestLifecycleAware()
    listener(startableTestLifecycleAware.perTest())

    "beforeTestCount for first test should be one" {
        startableTestLifecycleAware.beforeTestCount shouldBe 1
    }

    "beforeTestCount for second test should be two" {
        startableTestLifecycleAware.beforeTestCount shouldBe 2
    }
})
