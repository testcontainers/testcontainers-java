package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.CommandLine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@MethodSource("parameters")
public class DockerComposeProfilesOptionTest {

    public static Boolean[] local() {
        return new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    }

    @Parameter(0)
    public boolean localMode;

    public static final File COMPOSE_FILE = new File("src/test/resources/compose-profile-option/compose-test.yml");

    @BeforeEach
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
    public void testProfileOption() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(COMPOSE_FILE)
                .withOptions("--profile=cache")
                .withLocalCompose(this.localMode)
        ) {
            compose.start();
            assertThat(compose.listChildContainers()).hasSize(1);
        }
    }
}
