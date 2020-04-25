package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.lifecycle.Startable
import org.testcontainers.lifecycle.TestLifecycleAware


/**
 * [StartablePerSpecListener] starts the given [startable] before execution of any test in the spec
 * and stops after execution of all tests. If the [startable] also inherit from [TestLifecycleAware]
 * then its [beforeTest] and [afterTest] method are also called by the listener.
 *
 * [startable] can any of [GenericContainer] [DockerComposeContainer] [LocalStackContainer] etc.
 *
 * This listener should be used when you want to use a single container for all tests in a single spec class.
 *
 * @see
 * [StartablePerTestListener]
 * */

internal class StartablePerSpecListener(private val startable: Startable) : TestListener {
    private val testLifecycleAwareListener = TestLifecycleAwareListener(startable)

    override suspend fun beforeSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            startable.start()
        }
    }

    override suspend fun beforeTest(testCase: TestCase) {
        withContext(Dispatchers.IO) {
            testLifecycleAwareListener.beforeTest(testCase)
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            startable.stop()
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            testLifecycleAwareListener.afterTest(testCase, result)
        }
    }
}
