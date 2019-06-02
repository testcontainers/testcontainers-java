package com.example.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch

@Service
class Consumer {

    val latch = CountDownLatch(1)

    private val log: Logger = LoggerFactory.getLogger(Producer::class.java)

    @KafkaListener(topics = ["test-topic"])
    fun consume(message: String) {
        log.info("Consumed message::$message")
        latch.countDown()
    }
}