package org.testcontainers.junit;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the options associated with the docker-compose command.
 */
class ComposeContainerWithOptionsTest {

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
        ComposeContainer environment;
        if (localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(ComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker executable exists")
                .isTrue();
            environment = new ComposeContainer(composeFile).withOptions(options.stream().toArray(String[]::new));
        } else {
            environment =
                new ComposeContainer(DockerImageName.parse("docker:25.0.2"), composeFile)
                    .withOptions(options.stream().toArray(String[]::new));
        }

        try {
            environment.start();
            assertThat(expectError).isFalse();
            environment.stop();
        } catch (Exception e) {
            assertThat(expectError).isTrue();
        }
    }
}
