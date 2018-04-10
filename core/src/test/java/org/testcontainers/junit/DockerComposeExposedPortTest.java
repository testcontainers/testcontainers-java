package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.rnorth.visibleassertions.VisibleAssertions.pass;

public class DockerComposeExposedPortTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-exposedport-test.yml"))
        .withExposedService("redis_1", 6379);

    @Test
    public void test() {
        pass("container with exposed port created");
    }
}
