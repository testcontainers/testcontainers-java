package com.example.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ExampleController(
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        const val key = "testcontainers"
    }

    @PostMapping("/set-foo")
    fun setFoo(@RequestBody value: String) {
        redisTemplate.opsForValue().set(key, value)
    }

    @GetMapping("/get-foo")
    fun getFoo(): String? {
        return redisTemplate.opsForValue().get(key)
    }
}
