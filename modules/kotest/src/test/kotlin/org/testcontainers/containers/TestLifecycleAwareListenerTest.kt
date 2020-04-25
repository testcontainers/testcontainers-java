package org.testcontainers.containers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TestLifecycleAwareListenerTest : StringSpec({
    val startableTestLifecycleAware = StartableTestLifecycleAware()
    val testLifecycleAwareListener = TestLifecycleAwareListener(startableTestLifecycleAware)

    listener(testLifecycleAwareListener)

    "test id in test description should be combination of test name and package name" {
        val testDescription = startableTestLifecycleAware.testDescriptions[0]
        testDescription?.testId shouldBe "org.testcontainers.containers.TestLifecycleAwareListenerTest/test id in test" +
            " description should be combination of test name and package name"
    }

    "fileSystemFriendlyName .. in /// test description should be encoded test name" {
        val testDescription = startableTestLifecycleAware.testDescriptions[1]
        val encodedTestName = "fileSystemFriendlyName+..+in+%2F%2F%2F+test+description+should+be+encoded+test+name"

        testDescription?.filesystemFriendlyName shouldBe encodedTestName
    }
})
