package org.testcontainers.containers

import org.testcontainers.lifecycle.TestDescription
import org.testcontainers.lifecycle.TestLifecycleAware
import java.util.*

internal class StartableTestLifecycleAware : TestStartable(), TestLifecycleAware {
    var beforeTestCount = 0

    override fun beforeTest(description: TestDescription?) {
        beforeTestCount++
    }

    override fun afterTest(description: TestDescription?, throwable: Optional<Throwable>?) {
    }
}
