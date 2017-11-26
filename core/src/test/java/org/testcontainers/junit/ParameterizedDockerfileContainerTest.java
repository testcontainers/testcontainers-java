package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Simple test case / demonstration of creating a fresh container image from a Dockerfile DSL when the test
 * is parameterized.
 */
@RunWith(Parameterized.class)
public class ParameterizedDockerfileContainerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][] {
                { "alpine:3.2", "3.2"},
                { "alpine:3.3", "3.3"},
                { "alpine:3.4", "3.4"}
        };
    }

    public ParameterizedDockerfileContainerTest(String baseImage, String expectedVersion) {
        container = new GenericContainer(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> {
                builder
                        .from(baseImage)
                        .build();
            })).withCommand("sleep", "30");
        this.expectedVersion = expectedVersion;
    }

    @Rule
    public GenericContainer container;

    private final String expectedVersion;

    @Test
    public void simpleTest() throws Exception {
        final String release = container.execInContainer("cat", "/etc/alpine-release").getStdout();

        assertTrue("/etc/alpine-release starts with " + expectedVersion,
                release.startsWith(expectedVersion));
    }
}
