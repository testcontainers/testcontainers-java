package org.testcontainers.containers

import org.testcontainers.lifecycle.Startable

fun Startable.perTest() = StartablePerTestListener(this)
fun Startable.perSpec() = StartablePerSpecListener(this)
