package org.testcontainers.providers.kubernetes.intents;

import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.controller.model.BindMode;

public class CopyArchiveToContainerK8sIntentTest {


    @Test
    public void testFoo() {
        try(
            GenericContainer<?> container = new GenericContainer("nginx")
                .withClasspathResourceMapping("test.txt", "/tmp/config/test.txt", BindMode.READ_ONLY)
        ) {
            container.start();

            return;
        }
    }

}
