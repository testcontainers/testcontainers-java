package org.testcontainers.junit;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.ALPINE_IMAGE;

/**
 * Created by rnorth on 26/07/2016.
 */
public class WorkingDirectoryTest {

    @ClassRule
    public static GenericContainer container = new GenericContainer(ALPINE_IMAGE)
            .withWorkingDirectory("/etc")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
            .withCommand("ls", "-al");

    @Test
    public void checkOutput() {
        String listing = container.getLogs();

        assertTrue("Directory listing contains expected /etc content", listing.contains("hostname"));
        assertTrue("Directory listing contains expected /etc content", listing.contains("init.d"));
        assertTrue("Directory listing contains expected /etc content", listing.contains("passwd"));
    }

}
