package org.testcontainers.containers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TestLifecycleAwareListenerTest : StringSpec({
    val startableTestLifecycleAware = StartableTestLifecycleAware()
    listener(startableTestLifecycleAware.perTest())

    "beforeTestCount for first test should be one" {
        startableTestLifecycleAware.testDescriptions shouldHaveSize 1
    }

    "beforeTestCount for second test should be two" {
        startableTestLifecycleAware.testDescriptions shouldHaveSize 2
    }

    "test id in test description should be combination of test name and package name" {
        val testDescription = startableTestLifecycleAware.testDescriptions[2]
        testDescription?.testId shouldBe "org.testcontainers.containers.TestLifecycleAwareListenerTest/test id in test" +
            " description should be combination of test name and package name"
    }

    "fileSystemFriendlyName in test description should be encoded test name" {
        val testDescription = startableTestLifecycleAware.testDescriptions[3]
        val encodedTestName = "fileSystemFriendlyName+in+test+description+should+be+encoded+test+name"

        testDescription?.filesystemFriendlyName shouldBe encodedTestName
    }
})
