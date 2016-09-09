package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Autowired
	StringRedisTemplate stringRedisTemplate;

	@RequestMapping(value = "/foo", method = RequestMethod.GET)
	public String get() {
		return stringRedisTemplate.opsForValue().get("foo");
	}

	@RequestMapping(value = "/foo", method = RequestMethod.PUT)
	public void set(@RequestBody String value) {
		stringRedisTemplate.opsForValue().set("foo", value);
	}
}
