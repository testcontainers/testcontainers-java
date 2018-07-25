package com.example;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class DemoController {

    private final StringRedisTemplate stringRedisTemplate;

    public DemoController(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/foo")
    public String get() {
        return stringRedisTemplate.opsForValue().get("foo");
    }

    @PutMapping("/foo")
    public void set(@RequestBody String value) {
        stringRedisTemplate.opsForValue().set("foo", value);
    }
}
