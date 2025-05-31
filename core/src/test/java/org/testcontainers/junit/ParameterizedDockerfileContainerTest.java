package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit4.TestcontainersRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test case / demonstration of creating a fresh container image from a Dockerfile DSL when the test
 * is parameterized.
 */
@RunWith(Parameterized.class)
public class ParameterizedDockerfileContainerTest {

    private final String expectedVersion;

    @Rule
    public TestcontainersRule<GenericContainer<?>> container;

    public ParameterizedDockerfileContainerTest(String baseImage, String expectedVersion) {
        container =
            new TestcontainersRule<>(
                new GenericContainer<>(
                    new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder -> {
                            builder
                                .from(baseImage)
                                // Could potentially customise the image here, e.g. adding files, running
                                //  commands, etc.
                                .build();
                        })
                )
                    .withCommand("top")
            );
        this.expectedVersion = expectedVersion;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][] { //
            { "alpine:3.12", "3.12" },
            { "alpine:3.13", "3.13" },
            { "alpine:3.14", "3.14" },
            { "alpine:3.15", "3.15" },
            { "alpine:3.16", "3.16" },
        };
    }

    @Test
    public void simpleTest() throws Exception {
        final String release = container.get().execInContainer("cat", "/etc/alpine-release").getStdout();

        assertThat(release).as("/etc/alpine-release starts with " + expectedVersion).startsWith(expectedVersion);
    }
}
