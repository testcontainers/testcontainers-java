package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.lifecycle.Startable


/**
 * [StartablePerTestListener] starts the given [startable] before execution of each test in the spec
 * and stops after execution of each test.
 *
 * [startable] can any of [GenericContainer] [DockerComposeContainer] [LocalStackContainer] etc.
 *
 * This should be use when you want fresh container for each test.
 *
 * @see[StartablePerSpecListener]
 * */
internal class StartablePerTestListener(private val startable: Startable) : TestListener {

    override suspend fun beforeTest(testCase: TestCase) {
        withContext(Dispatchers.IO) {
            startable.start()
        }
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        withContext(Dispatchers.IO) {
            startable.stop()
        }
    }
}
