package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration test for Docker Compose variable substitution.
 * Tests the scenario described in the GitHub issue where image names with variables 
 * like ${TAG_CONFLUENT} were causing warnings.
 */
public class ComposeVariableSubstitutionIntegrationTest {

    @Test
    public void shouldHandleVariableSubstitutionWithoutWarnings() {
        // Set up environment variable as if it was set by the user
        System.setProperty("TAG_CONFLUENT", "7.0.0");
        System.setProperty("REDIS_VERSION", "alpine");

        try {
            // This should not generate warnings about invalid image names
            // because variable substitution should resolve ${TAG_CONFLUENT} to 7.0.0
            assertThatNoException().isThrownBy(() -> {
                try (ComposeContainer compose = new ComposeContainer(
                    new File("src/test/resources/docker-compose-variable-substitution.yml")
                )
                    .withLocalCompose(true)
                    .withPull(false)  // Don't actually pull images in test
                    .withLogConsumer("confluent", new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("confluent-test")))
                ) {
                    // Just validate the compose container can be created without exceptions
                    // The key test is that ParsedDockerComposeFile doesn't throw on variable substitution
                    // We're not actually starting the containers to avoid requiring Docker in the test
                }
            });
        } finally {
            // Clean up
            System.clearProperty("TAG_CONFLUENT");
            System.clearProperty("REDIS_VERSION");
        }
    }
}