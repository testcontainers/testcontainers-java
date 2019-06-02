package com.example.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service


@Service
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class Producer {

    private val log: Logger = LoggerFactory.getLogger(Producer::class.java)

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>

    fun sendMessage(topicName: String, message: String) {
        log.info("Producing message::$message")
        this.kafkaTemplate.send(topicName, message)
    }
}