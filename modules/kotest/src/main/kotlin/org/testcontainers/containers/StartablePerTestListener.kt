package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.lifecycle.Startable
import org.testcontainers.lifecycle.TestLifecycleAware


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
    private val testLifecycleAwareListener = TestLifecycleAwareListener(startable)

    override suspend fun beforeTest(testCase: TestCase) {
        withContext(Dispatchers.IO) {
            testLifecycleAwareListener.beforeTest(testCase)
            startable.start()
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            testLifecycleAwareListener.afterTest(testCase, result)
            startable.stop()
        }
    }
}

