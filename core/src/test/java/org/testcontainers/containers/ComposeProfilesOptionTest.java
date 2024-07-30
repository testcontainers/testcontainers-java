package org.testcontainers.containers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ComposeProfilesOptionTest {

    @Parameterized.Parameters(name = "{0}")
    public static Boolean[] local() {
        return new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    }

    @Parameterized.Parameter
    public boolean local;

    public static final File COMPOSE_FILE = new File("src/test/resources/compose-profile-option/compose-test.yml");

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
