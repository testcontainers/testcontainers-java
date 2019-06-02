package com.example.kafka

import org.junit.ClassRule
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.KafkaContainer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.test.context.ContextConfiguration
import java.util.*


@RunWith(SpringRunner::class)
@ContextConfiguration(initializers = [AbstractIntegrationTest.Initializer::class])
abstract class AbstractIntegrationTest {

    companion object {
        @ClassRule
        @JvmField
        val kafkaContainer = KafkaContainer()
    }


    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.kafka.consumer.bootstrap-servers=" + kafkaContainer.bootstrapServers,
                "spring.kafka.consumer.group-id=" + UUID.randomUUID(),
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",

                "spring.kafka.producer.bootstrap-servers=" + kafkaContainer.bootstrapServers,
                "spring.kafka.producer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.producer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
