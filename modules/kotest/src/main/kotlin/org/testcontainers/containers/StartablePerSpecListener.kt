package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.lifecycle.Startable


/**
 * [StartablePerSpecListener] starts the given [startable] before execution of any test in the spec
 * and stops after execution of all tests.
 *
 * [startable] can any of [GenericContainer] [DockerComposeContainer] [LocalStackContainer] etc.
 *
 * This listener should be used when you want to use a single container for all tests in a single spec class.
 *
 * @see
 * [StartablePerTestListener]
 * */

internal class StartablePerSpecListener(private val startable: Startable) : TestListener {
    override suspend fun beforeSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            startable.start()
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        withContext(Dispatchers.IO) {
            startable.stop()
        }
    }
}
