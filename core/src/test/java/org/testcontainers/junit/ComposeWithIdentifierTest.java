package org.testcontainers.junit;

import org.junit.Rule;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit4.TestcontainersRule;

import java.io.File;

public class ComposeWithIdentifierTest extends BaseComposeTest {

    @Rule
    public TestcontainersRule<ComposeContainer> environment = new TestcontainersRule<>(
        new ComposeContainer("TEST", new File("src/test/resources/v2-compose-test.yml"))
            .withExposedService("redis-1", REDIS_PORT)
    );

    @Override
    protected ComposeContainer getEnvironment() {
        return this.environment.get();
    }
}
