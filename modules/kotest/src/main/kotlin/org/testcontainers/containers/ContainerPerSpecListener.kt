package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import kotlin.reflect.KClass


/**
 * [ContainerPerSpecListener] starts the given [genericContainers] before execution of any test in the spec
 * and stops after execution of all tests.
 *
 * [genericContainers] can any of [GenericContainer] [ContainerisedDockerCompose] [LocalStackContainer] etc.
 *
 * This should be use when you want to a single container for all test in a single test class.
 *
 * @see
 * [ContainerPerSpecListener]
 *
 * Usage:
 *
 * class RedisRepositoryTest: StringSpec() {
 *    private val redisContainer = GenericContainer<Nothing>("redis")
 *
 *    override fun listeners(): List<TestListener> {
 *       return super.listeners() + ContainerPerSpecListener(redisContainer)
 *    }
 *
 *    init {
 *       "should be able to connect redis server" {
 *         // your test goes here
 *       }
 *    }
 * }
 * */

class ContainerPerSpecListener(private vararg val genericContainers: GenericContainer<Nothing>) : TestListener {

   override suspend fun finalizeSpec(kclass: KClass<out Spec>, results: Map<TestCase, TestResult>) {
       genericContainers.forEach { it.stop() }
      super.finalizeSpec(kclass, results)
   }

   override suspend fun prepareSpec(kclass: KClass<out Spec>) {
       genericContainers.forEach { it.start() }
      super.prepareSpec(kclass)
   }
}
