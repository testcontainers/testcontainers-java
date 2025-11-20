package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

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
        ComposeContainer compose;
        if (localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(ComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker executable exists")
                .isTrue();
            // composeContainerWithLocalCompose {
            compose =
                new ComposeContainer(COMPOSE_FILE)
                    // }
                    .withOptions("--profile=cache");
        } else {
            compose =
                new ComposeContainer(DockerImageName.parse("docker:25.0.2"), COMPOSE_FILE)
                    .withOptions("--profile=cache");
        }
        compose.start();
        assertThat(compose.listChildContainers()).hasSize(1);
        compose.stop();
    }
}
