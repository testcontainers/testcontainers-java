package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.lifecycle.TestDescription
import org.testcontainers.lifecycle.TestLifecycleAware
import java.util.*

internal class TestLifecycleAwareListener(private val testLifecycleAware: TestLifecycleAware) : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        withContext(Dispatchers.IO) {
            testLifecycleAware.beforeTest(testCase.toTestDescription())
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            testLifecycleAware.afterTest(testCase.toTestDescription(), Optional.ofNullable(result.error))
        }
    }
}

private fun TestCase.toTestDescription() = object : TestDescription {

    override fun getFilesystemFriendlyName(): String {
        return name.replace(" ", "-")
    }

    override fun getTestId(): String {
        return description.id()
    }
}

