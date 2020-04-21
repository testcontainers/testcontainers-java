package org.testcontainers.containers

import io.kotest.core.listeners.TestListener
import org.testcontainers.lifecycle.Startable
import org.testcontainers.lifecycle.TestLifecycleAware

fun Startable.perTest(): TestListener = StartablePerTestListener(this)
fun Startable.perSpec(): TestListener = StartablePerSpecListener(this)
fun TestLifecycleAware.perTest(): TestListener = TestLifecycleAwareListener(this)
