package org.testcontainers.redis;

import com.google.common.collect.Lists;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * The redis container.
 * <pre>{@code
 *    RedisContainer redis = new RedisContainer().withPort(6666);
 *    redis.start();
 *
 *    Jedis jedis = new Jedis(redis.getHost(), 6666);
 *    jedis.ping();
 * }</pre>
 */
public class RedisContainer extends GenericContainer<RedisContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
    private static final String DEFAULT_TAG = "6.2";
    private final int DEFAULT_REDIS_PORT = 6379;

    private int redisPort = DEFAULT_REDIS_PORT;

    public RedisContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public RedisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);

        waitingFor(
            new LogMessageWaitStrategy()
                .withRegEx(".*Ready to accept connections.*")
                .withStartupTimeout(Duration.ofMinutes(1))
        );
    }

    @Override
    protected void configure() {
        // expose the redis port so that we can connect to it.
        setPortBindings(Lists.newArrayList(redisPort + ":" + DEFAULT_REDIS_PORT));
    }

    /**
     * Set the redis port, if you don't set, the default port that you can connect is 6379.
     *
     * @param redisPort redis port
     * @return this
     */
    public RedisContainer withPort(int redisPort) {
        this.redisPort = redisPort;
        return this;
    }
}
