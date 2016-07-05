package org.testcontainers.junit;

import org.junit.Test;
import org.junit.runner.Description;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeErrorHandlingTest {


    @Test
    public void simpleTest() {

        DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/invalid-compose.yml"))
                    .withExposedService("something", 123);

        VisibleAssertions.assertThrows("starting with an invalid docker-compose file throws an exception",
                ContainerLaunchException.class,
                () -> {
                    environment.starting(Description.createTestDescription(Object.class, "name"));
                });
    }
}
