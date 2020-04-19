package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.testcontainers.lifecycle.Startable


/**
 * [StartablePerTestListener] starts the given [startables] before execution of each test in the spec
 * and stops after execution of each test.
 *
 * [startables] can any of [GenericContainer] [ContainerisedDockerCompose] [LocalStackContainer] etc.
 *
 * This should be use when you want fresh container for each test.
 *
 * @see[StartablePerSpecListener]
 * */
class StartablePerTestListener(private vararg val startables: Startable) : TestListener {

   override suspend fun beforeTest(testCase: TestCase) {
      startables.forEach { it.start() }
      super.beforeTest(testCase)
   }

   override suspend fun afterTest(testCase: TestCase, result: TestResult) {
       startables.forEach { it.stop() }
      super.afterTest(testCase, result)
   }
}
