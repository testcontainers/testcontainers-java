package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.utility.CommandLine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ComposeProfilesOptionTest {

    @Parameterized.Parameters(name = "{0}")
    public static Boolean[] local() {
        return new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    }

    @Parameterized.Parameter
    public boolean localMode;

    public static final File COMPOSE_FILE = new File("src/test/resources/compose-profile-option/compose-test.yml");

    @Before
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
            ComposeContainer compose = new ComposeContainer(COMPOSE_FILE)
                .withOptions("--profile=cache")
                .withLocalCompose(true)
        ) {
            compose.start();
            assertThat(compose.listChildContainers()).hasSize(1);
        }
    }
}
