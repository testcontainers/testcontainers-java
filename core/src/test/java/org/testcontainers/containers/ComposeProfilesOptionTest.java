package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.testcontainers.utility.CommandLine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@MethodSource("local")
public class ComposeProfilesOptionTest {

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
                .assumeThat(CommandLine.executableExists(ComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker executable exists")
                .isTrue();
        }
    }

    @Test
    public void testProfileOption() {
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
