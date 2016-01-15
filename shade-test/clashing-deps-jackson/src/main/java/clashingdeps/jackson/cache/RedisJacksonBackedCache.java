package clashingdeps.jackson.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Optional;

/**
 * An implementation of {@link Cache} that stores data in Redis.
 *
 * This implementation uses Jackson for JSON serialization/deserialization.
 */
public class RedisJacksonBackedCache implements Cache {

    private final Jedis jedis;
    private final String cacheName;
    private final ObjectMapper objectMapper;

    public RedisJacksonBackedCache(Jedis jedis, String cacheName) {
        this.jedis = jedis;
        this.cacheName = cacheName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void put(String key, Object value) {
        String jsonValue = null;
        try {
            jsonValue = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // Do something with this in reality
        }
        this.jedis.hset(this.cacheName, key, jsonValue);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> expectedClass) {
        String foundJson = this.jedis.hget(this.cacheName, key);

        if (foundJson == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(foundJson, expectedClass));
        } catch (IOException e) {
            return Optional.empty();
            // Do something with this in reality
        }
    }
}
