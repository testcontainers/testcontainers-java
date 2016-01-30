package org.testcontainers.junit;

import org.junit.ClassRule;
import org.junit.Test;
import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.core.RAtomicLong;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerTest {

    private static final int REDIS_PORT = 6379;

    @ClassRule
    public static DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT);

    @Test
    public void simpleTest() {
        Config config = new Config();
        config.useSingleServer().setAddress(environment.getServiceHost("redis_1", REDIS_PORT) + ":" + environment.getServicePort("redis_1", REDIS_PORT));
        Redisson redisson = Redisson.create(config);

        RAtomicLong test = redisson.getAtomicLong("test");
        test.incrementAndGet();
        test.incrementAndGet();
        test.incrementAndGet();

        assertEquals("A redis instance defined in compose can be used", 3, (int) test.get());
    }
}
