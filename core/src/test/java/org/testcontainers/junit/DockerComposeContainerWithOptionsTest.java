package org.testcontainers.junit;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assumptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.CommandLine;

import java.io.File;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the options associated with the docker-compose command.
 */
@RunWith(Parameterized.class)
public class DockerComposeContainerWithOptionsTest {

    public DockerComposeContainerWithOptionsTest(
        final File composeFile,
        final boolean localMode,
        final Set<String> options,
        final boolean expectError
    ) {
        this.composeFile = composeFile;
        this.localMode = localMode;
        this.options = options;
        this.expectError = expectError;
    }

    private final File composeFile;

    private final boolean localMode;

    private final Set<String> options;

    private final boolean expectError;

    @Parameterized.Parameters(
        name = "docker-compose test [compose file: {0}, local: {1}, options: {2}, expected result: {3}]"
    )
    public static Object[][] params() {
        return new Object[][] {
            // Test the happy day case. THe compatibility option should be accepted by docker-compose.
            {
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                false,
                ImmutableSet.of("--compatibility"),
                false,
            },
            // Test with flags absent. Docker compose will warn but continue, ignoring the deploy block.
            {
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                false,
                ImmutableSet.of(""),
                false,
            },
            // Test with a bad option. Compose will complain.
            {
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                false,
                ImmutableSet.of("--bad-option"),
                true,
            },
            // Local compose
            {
                new File("src/test/resources/compose-options-test/with-deploy-block.yml"),
                true,
                ImmutableSet.of("--compatibility"),
                false,
            },
        };
    }

    @Before
    public void setUp() {
        if (this.localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(DockerComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker-compose executable exists")
                .isTrue();
            Assumptions
                .assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
                .doesNotStartWith("Docker Compose version v2");
        }
    }

    @Test
    public void performTest() {
        try (
            DockerComposeContainer<?> environment = new DockerComposeContainer<>(composeFile)
                .withOptions(options.stream().toArray(String[]::new))
                .withLocalCompose(localMode)
        ) {
            environment.start();
            assertThat(expectError).isEqualTo(false);
        } catch (Exception e) {
            assertThat(expectError).isEqualTo(true);
        }
    }
}
