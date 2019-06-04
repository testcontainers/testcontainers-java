package com.example.redis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RedisApplication

fun main(args: Array<String>) {
    runApplication<RedisApplication>(*args)
}
