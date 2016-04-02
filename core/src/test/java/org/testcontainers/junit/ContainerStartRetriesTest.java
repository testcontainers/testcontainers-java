package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.rnorth.visibleassertions.VisibleAssertions.pass;

public class ContainerStartRetriesTest {

    @ClassRule
    public static GenericContainer container = new GenericContainer() {

        volatile boolean firstAttempt = true;

        @Override
        protected void configure() {
            super.configure();

            withCommand("ps");
            withExposedPorts(80);
            withCommand("/bin/sh", "-c", "while true; do cat /content.txt | nc -l -p 80; done");

            // Mapping should be called only once, otherwise Docker will fail with "Duplicate mount point '/content.txt'"
            withClasspathResourceMapping("mappable-resource/test-resource.txt", "/content.txt", READ_ONLY);
        }

        @Override
        protected void containerIsStarting(InspectContainerResponse containerInfo) {
            if (firstAttempt) {
                firstAttempt = false;

                throw new IllegalStateException("Could not start container on the first attempt");
            }

            super.containerIsStarting(containerInfo);
        }
    };

    @Test
    public void configureMethodExecutedOnlyOnce() {
        pass("Container didn't fail");
    }
}
