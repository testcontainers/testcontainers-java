package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec


/**
 * [ContainerPerSpecListener] starts the given [genericContainers] before execution of any test in the spec
 * and stops after execution of all tests.
 *
 * [genericContainers] can any of [GenericContainer] [ContainerisedDockerCompose] [LocalStackContainer] etc.
 *
 * This should be use when you want to a single container for all test in a single test class.
 *
 * @see
 * [ContainerPerTestListener]
 * */

class ContainerPerSpecListener(private vararg val genericContainers: GenericContainer<Nothing>) : TestListener {
    override suspend fun beforeSpec(spec: Spec) {
        genericContainers.forEach { it.start() }
        super.beforeSpec(spec)
    }

    override suspend fun afterSpec(spec: Spec) {
        genericContainers.forEach { it.stop() }
        super.afterSpec(spec)
    }
}
