package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import org.testcontainers.lifecycle.Startable


/**
 * [StartablePerSpecListener] starts the given [startables] before execution of any test in the spec
 * and stops after execution of all tests.
 *
 * [startables] can any of [GenericContainer] [ContainerisedDockerCompose] [LocalStackContainer] etc.
 *
 * This should be use when you want to a single container for all test in a single test class.
 *
 * @see
 * [StartablePerTestListener]
 * */

class StartablePerSpecListener(private vararg val startables: Startable) : TestListener {
    override suspend fun beforeSpec(spec: Spec) {
        startables.forEach { it.start() }
        super.beforeSpec(spec)
    }

    override suspend fun afterSpec(spec: Spec) {
        startables.forEach { it.stop() }
        super.afterSpec(spec)
    }
}
