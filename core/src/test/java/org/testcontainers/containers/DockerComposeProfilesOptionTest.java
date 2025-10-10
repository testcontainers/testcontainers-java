package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeProfilesOptionTest {

    public static Boolean[] local() {
        return new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    }

    public static final File COMPOSE_FILE = new File("src/test/resources/compose-profile-option/compose-test.yml");

    @ParameterizedTest(name = "{0}")
    @MethodSource("local")
    void testProfileOption(boolean localMode) {
        DockerComposeContainer<?> compose;
        if (localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(DockerComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker-compose executable exists")
                .isTrue();
            Assumptions
                .assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
                .doesNotStartWith("Docker Compose version v2");

            compose = new DockerComposeContainer<>(COMPOSE_FILE).withOptions("--profile=cache");
        } else {
            compose =
                new DockerComposeContainer<>(DockerImageName.parse("docker/compose:debian-1.29.2"), COMPOSE_FILE)
                    .withOptions("--profile=cache");
        }

        compose.start();
        assertThat(compose.listChildContainers()).hasSize(1);
        compose.stop();
    }
}
