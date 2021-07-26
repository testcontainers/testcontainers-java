package com.example.redis

import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.GenericContainer

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [AbstractIntegrationTest.Initializer::class])
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    companion object {
        val redisContainer = GenericContainer<Nothing>("redis:3-alpine")
                .apply { withExposedPorts(6379) }
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            redisContainer.start()

            TestPropertyValues.of(
                "spring.redis.host=${redisContainer.containerIpAddress}",
                "spring.redis.port=${redisContainer.firstMappedPort}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
