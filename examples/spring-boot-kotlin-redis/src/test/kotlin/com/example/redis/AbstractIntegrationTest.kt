package com.example.redis

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer

@SpringBootTest
@ContextConfiguration(initializers = [AbstractIntegrationTest.Initializer::class])
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    companion object {
        val redisContainer = GenericContainer<Nothing>("redis:6-alpine")
                .apply { withExposedPorts(6379) }
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            redisContainer.start()

            TestPropertyValues.of(
                "spring.redis.host=${redisContainer.host}",
                "spring.redis.port=${redisContainer.firstMappedPort}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
