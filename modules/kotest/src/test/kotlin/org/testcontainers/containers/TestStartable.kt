package org.testcontainers.containers

import org.testcontainers.lifecycle.Startable

internal open class TestStartable : Startable {
    var startCount = 0
    override fun start() {
        startCount++
    }
    override fun stop() {
    }
}
