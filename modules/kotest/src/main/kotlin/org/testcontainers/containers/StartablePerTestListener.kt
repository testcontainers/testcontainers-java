package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.lifecycle.Startable
import org.testcontainers.lifecycle.TestDescription
import org.testcontainers.lifecycle.TestLifecycleAware
import java.net.URLEncoder
import java.util.*


/**
 * [StartablePerTestListener] starts the given [startable] before execution of each test in the spec
 * and stops after execution of each test. If the [startable] also inherit from [TestLifecycleAware]
 * then its [beforeTest] and [afterTest] method are also called by the listener.
 *
 * [startable] can any of [GenericContainer] [DockerComposeContainer] [LocalStackContainer] etc.
 *
 * This should be used when you want a fresh container for each test.
 *
 * @see[StartablePerSpecListener]
 * */
internal class StartablePerTestListener(private val startable: Startable) : TestListener {

    override suspend fun beforeTest(testCase: TestCase) {
        withContext(Dispatchers.IO) {
            runBeforeTestForTestLifecycleAware(testCase)
            startable.start()
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            runAfterTestForTestLifecycleAware(testCase, result)
            startable.stop()
        }
    }

    private fun runBeforeTestForTestLifecycleAware(testCase: TestCase) {
        startable.toTestLifecycleAware()?.beforeTest(testCase.toTestDescription())
    }

    private fun runAfterTestForTestLifecycleAware(testCase: TestCase, result: TestResult) {
        startable.toTestLifecycleAware()?.afterTest(testCase.toTestDescription(), Optional.ofNullable(result.error))
    }

    private fun Startable.toTestLifecycleAware() = this as? TestLifecycleAware
}


private fun TestCase.toTestDescription() = object : TestDescription {

    override fun getFilesystemFriendlyName(): String {
        return URLEncoder.encode(name, "UTF-8")
    }

    override fun getTestId(): String {
        return description.id()
    }
}

