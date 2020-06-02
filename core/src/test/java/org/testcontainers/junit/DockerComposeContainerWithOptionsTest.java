package org.testcontainers.junit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the options associated with the docker-compose command.
 */
@RunWith(Parameterized.class)
public class DockerComposeContainerWithOptionsTest {

    public DockerComposeContainerWithOptionsTest(final File composeFile, final boolean local, final String options, final boolean expectError) {
        this.composeFile = composeFile;
        this.local = local;
        this.options = options;
        this.expectError = expectError;
    }

    private final File composeFile;
    private final boolean local;
    public final String options;
    private final boolean expectError;

    @Parameterized.Parameters(name = "docker-compose test [compose file: {0}, local: {1}, option: {2}, expected result: {3}]")
    public static Object[][] params() {
        return new Object[][]{
            // Test the happy day case. THe compatibility option should be accepted by docker-compose.
            {new File("src/test/resources/compose-options-test/with-deploy-block.yml"), false, "--compatibility", false},
            // Test with flags absent. Docker compose will warn but continue, ignoring the deploy block.
            {new File("src/test/resources/compose-options-test/with-deploy-block.yml"), false, "", false},
            // Test with a bad option. Compose will complain.
            {new File("src/test/resources/compose-options-test/with-deploy-block.yml"), false, "--bad-option", true},
            // Local compose
            {new File("src/test/resources/compose-options-test/with-deploy-block.yml"), true, "--compatibility", false},
        };
    }

    @Test
    public void performTest() {

        try (DockerComposeContainer<?> environment = new DockerComposeContainer<>(composeFile)
                .withOptions(options)
                .withLocalCompose(local)) {
            environment.start();
            assertThat(expectError).isEqualTo(false);
        } catch (Exception e) {
            assertThat(expectError).isEqualTo(true);
        }

    }

}
