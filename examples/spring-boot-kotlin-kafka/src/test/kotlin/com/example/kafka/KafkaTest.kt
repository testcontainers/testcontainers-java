package com.example.kafka

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.rule.OutputCapture
import java.util.concurrent.TimeUnit


@SpringBootTest
class KafkaTest : AbstractIntegrationTest() {

    @get:Rule
    val capture = OutputCapture()

    @Autowired
    private lateinit var consumer: Consumer

    @Autowired
    private lateinit var producer: Producer

    @Test
    fun testKafkaFunctionality() {
        val topicName = "test-topic"
        val greeting = "Hello TestContainers with Kotlin!"

        producer.sendMessage(topicName, greeting)

        assertThat(capture.toString()).containsSequence("Producing message::$greeting")

        consumer.latch.await(10, TimeUnit.SECONDS)

        assertThat(consumer.latch.count).isEqualTo(0)

        assertThat(capture.toString()).containsSequence("Consumed message::$greeting")
    }
}