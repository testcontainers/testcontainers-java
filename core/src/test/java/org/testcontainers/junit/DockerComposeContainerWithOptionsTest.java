package org.testcontainers.junit;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the options associated with the docker-compose command.
 */
class DockerComposeContainerWithOptionsTest {

    public static Stream<Arguments> params() {
        return Stream.of(
            // Test the happy day case. The compatibility option should be accepted by docker-compose.
            Arguments.of(
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                false,
                ImmutableSet.of("--compatibility"),
                false
            ),
            // Test with flags absent. Docker compose will warn but continue, ignoring the deploy block.
            Arguments.of(
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                false,
                ImmutableSet.of(""),
                false
            ),
            // Test with a bad option. Compose will complain.
            Arguments.of(
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                false,
                ImmutableSet.of("--bad-option"),
                true
            ),
            // Local compose
            Arguments.of(
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                true,
                ImmutableSet.of("--compatibility"),
                false
            )
        );
    }

    @ParameterizedTest(name = "docker-compose test [compose file: {0}, local: {1}, options: {2}, expected result: {3}]")
    @MethodSource("params")
    void performTest(File composeFile, boolean localMode, Set<String> options, boolean expectError) {
        DockerComposeContainer<?> environment;
        if (localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(DockerComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker-compose executable exists")
                .isTrue();
            Assumptions
                .assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
                .doesNotStartWith("Docker Compose version v2");

            environment =
                new DockerComposeContainer<>(composeFile).withOptions(options.stream().toArray(String[]::new));
        } else {
            environment =
                new DockerComposeContainer<>(DockerImageName.parse("docker/compose:debian-1.29.2"), composeFile)
                    .withOptions(options.stream().toArray(String[]::new));
        }

        try {
            environment.start();
            assertThat(expectError).isEqualTo(false);
            environment.stop();
        } catch (Exception e) {
            assertThat(expectError).isEqualTo(true);
        }
    }
}
