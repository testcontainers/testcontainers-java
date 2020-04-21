package org.testcontainers.containers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.lifecycle.TestDescription
import org.testcontainers.lifecycle.TestLifecycleAware

class TestLifecycleAwareListenerTest : StringSpec({
    val mockTestLifecycleAware = MockTestLifecycleAware()
    listener(mockTestLifecycleAware.perTest())

    "beforeTestCalledCount for first test should be one" {
        mockTestLifecycleAware.beforeTestCalledCount shouldBe 1
    }

    "beforeTestCalledCount for second test should be two" {
        mockTestLifecycleAware.beforeTestCalledCount shouldBe 2
    }
})

private class MockTestLifecycleAware : TestLifecycleAware {
    var beforeTestCalledCount = 0

    override fun beforeTest(description: TestDescription?) {
        beforeTestCalledCount++
    }
}
