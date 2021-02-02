package org.testcontainers.containers.output;

import static org.junit.Assert.assertFalse;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;

// Based on an idea by @ReuDa in
// https://github.com/testcontainers/testcontainers-java/issues/1854#issuecomment-703459478.
public class NewLineContainerTest {

    private static final AlpineContainer container = new AlpineContainer();

    @Test
    public void newlines_are_not_added_to_container_output() throws Exception {
        container.start();

        String largeString = RandomStringUtils.randomAlphabetic(10000);
        assertFalse(largeString.contains("\n"));

        ExecResult build = container.execInContainer("echo", "-n", largeString);
        assertFalse(build.getStdout().contains("\n"));
    }

    static class AlpineContainer extends GenericContainer<AlpineContainer> {
        AlpineContainer() {
            super("alpine");
        }

        @Override
        protected void configure() {
            super.configure();
            this.withCommand("sleep", "2m");
        }
    }
}
