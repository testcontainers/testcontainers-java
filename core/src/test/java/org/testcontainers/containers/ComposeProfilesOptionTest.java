package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.CommandLine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeProfilesOptionTest {

    public static Boolean[] local() {
        return new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    }

    public static final File COMPOSE_FILE = new File("src/test/resources/compose-profile-option/compose-test.yml");

    @ParameterizedTest
    @MethodSource("local")
    void testProfileOption(boolean localMode) {
        if (localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(ComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker executable exists")
                .isTrue();
        }
        try (
            // composeContainerWithLocalCompose {
            ComposeContainer compose = new ComposeContainer(COMPOSE_FILE)
                .withLocalCompose(true)
                // }
                .withOptions("--profile=cache")
        ) {
            compose.start();
            assertThat(compose.listChildContainers()).hasSize(1);
        }
    }
}
