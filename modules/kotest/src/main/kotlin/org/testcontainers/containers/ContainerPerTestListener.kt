package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult


/**
 * [ContainerPerTestListener] starts the given [genericContainers] before execution of each test in the spec
 * and stops after execution of each test.
 *
 * [genericContainers] can any of [GenericContainer] [ContainerisedDockerCompose] [LocalStackContainer] etc.
 *
 * This should be use when you want fresh container for each test.
 *
 * @see[ContainerPerSpecListener]
 * Usage:
 *
 * class RedisRepositoryTest: StringSpec() {
 *    private val redisContainer = GenericContainer<Nothing>("redis")
 *
 *    override fun listeners(): List<TestListener> {
 *       return super.listeners() + ContainerPerTestListener(redisContainer)
 *    }
 *
 *    init {
 *       "should be able to connect redis server" {
 *         // your test goes here
 *       }
 *    }
 * }
 * */
class ContainerPerTestListener(private vararg val genericContainers: GenericContainer<Nothing>) : TestListener {

   override suspend fun beforeTest(testCase: TestCase) {
      genericContainers.forEach { it.start() }
      super.beforeTest(testCase)
   }

   override suspend fun afterTest(testCase: TestCase, result: TestResult) {
       genericContainers.forEach { it.stop() }
      super.afterTest(testCase, result)
   }
}
