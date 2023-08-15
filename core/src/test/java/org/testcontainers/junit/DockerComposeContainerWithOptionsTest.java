package org.testcontainers.junit;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the options associated with the docker-compose command.
 */
public class DockerComposeContainerWithOptionsTest {

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            // Test the happy day case. THe compatibility option should be accepted by docker-compose.
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
    @MethodSource("provideParameters")
    public void performTest(File composeFile, boolean local, Set<String> options, boolean expectError) {
        try (
            DockerComposeContainer<?> environment = new DockerComposeContainer<>(composeFile)
                .withOptions(options.stream().toArray(String[]::new))
                .withLocalCompose(local)
        ) {
            environment.start();
            assertThat(expectError).isEqualTo(false);
        } catch (Exception e) {
            assertThat(expectError).isEqualTo(true);
        }
    }
}
