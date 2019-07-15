package com.example.redis

import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders


class RedisTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun testRedisFunctionality() {
        val greeting = "Hello Testcontainers with Kotlin"
        mockMvc.perform(post("/set-foo").content(greeting))
            .andExpect(status().isOk)

        mockMvc.perform(get("/get-foo"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString(greeting)))
    }
}
