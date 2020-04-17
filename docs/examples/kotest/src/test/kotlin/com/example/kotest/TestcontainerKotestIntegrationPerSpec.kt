package com.example.kotest

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import org.testcontainers.containers.ContainerPerSpecListener
import org.testcontainers.containers.GenericContainer

class TestcontainerKotestIntegrationPerSpec : StringSpec({
    val container = GenericContainer<Nothing>("ubuntu")
    setContainerCommand(container) // Set container command for first time to echo hello Kotest

    // Register the Listener
    listeners(ContainerPerSpecListener(container))

    "test should echo hello Kotest for first test" {
        container.logs.trim() shouldBe "hello Kotest"
        // Set container command so that next time when container start we will have hello Kotest again in container log
        setContainerCommand(container)
        delay(1000)
    }

    "test should echo hello Kotest for second spec as well" {
        // presence of "hello Kotest" verifies that container is not restarted.
        container.logs.trim() shouldBe "hello Kotest"
    }

})

private var testCount = 0
private fun setContainerCommand(container: GenericContainer<Nothing>) {
    if (testCount == 0) {
        container.setCommand("echo", "hello Kotest")
        testCount += 1
    } else {
        container.setCommand("echo", "hello Kotest again")
    }
}

