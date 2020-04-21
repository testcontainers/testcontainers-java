package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import kotlinx.coroutines.coroutineScope
import org.testcontainers.lifecycle.Startable


/**
 * [StartablePerSpecListener] starts the given [startable] before execution of any test in the spec
 * and stops after execution of all tests.
 *
 * [startable] can any of [GenericContainer] [DockerComposeContainer] [LocalStackContainer] etc.
 *
 * This should be use when you want to a single container for all test in a single test class.
 *
 * @see
 * [StartablePerTestListener]
 * */

class StartablePerSpecListener(private vararg val startable: Startable) : TestListener {
    override suspend fun beforeSpec(spec: Spec) {
        coroutineScope {
            startable.forEach { it.start() }
        }
        super.beforeSpec(spec)
    }

    override suspend fun afterSpec(spec: Spec) {
        startable.forEach { it.stop() }
        super.afterSpec(spec)
    }
}
