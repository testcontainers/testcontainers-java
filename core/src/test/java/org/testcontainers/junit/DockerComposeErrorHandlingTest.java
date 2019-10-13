package org.testcontainers.junit;

import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class DockerComposeErrorHandlingTest {

    @Test
    public void simpleTest() {
        VisibleAssertions.assertThrows("starting with an invalid docker-compose file throws an exception",
            IllegalArgumentException.class,
                () -> {
                    DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/invalid-compose.yml"))
                        .withExposedService("something", 123);
                });
    }
}
