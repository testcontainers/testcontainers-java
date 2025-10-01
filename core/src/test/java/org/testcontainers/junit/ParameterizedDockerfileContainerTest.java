package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test case / demonstration of creating a fresh container image from a Dockerfile DSL when the test
 * is parameterized.
 */
@ParameterizedClass(name = "{0}")
@MethodSource("data")
class ParameterizedDockerfileContainerTest {

    private final String expectedVersion;

    public GenericContainer container;

    public ParameterizedDockerfileContainerTest(String baseImage, String expectedVersion) {
        container =
            new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from(baseImage)
                            // Could potentially customise the image here, e.g. adding files, running
                            //  commands, etc.
                            .build();
                    })
            )
                .withCommand("top");
        container.start();
        this.expectedVersion = expectedVersion;
    }

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("alpine:3.12", "3.12"),
            Arguments.of("alpine:3.13", "3.13"),
            Arguments.of("alpine:3.14", "3.14"),
            Arguments.of("alpine:3.15", "3.15"),
            Arguments.of("alpine:3.16", "3.16")
        );
    }

    @Test
    void simpleTest() throws Exception {
        final String release = container.execInContainer("cat", "/etc/alpine-release").getStdout();

        assertThat(release).as("/etc/alpine-release starts with " + expectedVersion).startsWith(expectedVersion);
    }
}
