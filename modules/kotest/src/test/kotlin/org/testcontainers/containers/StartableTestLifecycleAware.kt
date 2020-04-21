package org.testcontainers.containers

import org.testcontainers.lifecycle.TestDescription
import org.testcontainers.lifecycle.TestLifecycleAware
import java.util.*

internal class StartableTestLifecycleAware : TestStartable(), TestLifecycleAware {
    val testDescriptions = mutableListOf<TestDescription?>()

    override fun beforeTest(description: TestDescription?) {
        testDescriptions.add(description)
    }

    override fun afterTest(description: TestDescription?, throwable: Optional<Throwable>?) {
    }
}
