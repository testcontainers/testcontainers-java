package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rnorth on 26/07/2016.
 */
class WorkingDirectoryTest {

    public static GenericContainer container = new GenericContainer(TestImages.ALPINE_IMAGE)
        .withWorkingDirectory("/etc")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        .withCommand("ls", "-al");

    static {
        container.start();
    }

    @Test
    void checkOutput() {
        String listing = container.getLogs();

        assertThat(listing).as("Directory listing contains expected /etc content").contains("hostname");
        assertThat(listing).as("Directory listing contains expected /etc content").contains("init.d");
        assertThat(listing).as("Directory listing contains expected /etc content").contains("passwd");
    }
}
