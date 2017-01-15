package org.testcontainers.dockerclient;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;

import static org.junit.Assume.assumeTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.junit.GenericContainerRuleTest.getReaderForContainerPort80;

public class DockerInDockerTest {

    @ClassRule
    public static TestRule assumption = new TestWatcher() {
        @Override
        public Statement apply(Statement base, Description description) {
            assumeTrue("We're inside a container", DockerClientConfigUtils.IN_A_CONTAINER);
            return super.apply(base, description);
        }
    };

    @Rule
    public GenericContainer container = new GenericContainer("alpine:3.2")
            .withExposedPorts(80)
            .withCommand("/bin/sh", "-c", "while true; do echo \"hello\" | nc -l -p 80; done");

    @Test
    public void testIpDetection() throws Exception {
        String line = getReaderForContainerPort80(container).readLine();
        assertEquals("The container is accessible", "hello", line);
    }
}
