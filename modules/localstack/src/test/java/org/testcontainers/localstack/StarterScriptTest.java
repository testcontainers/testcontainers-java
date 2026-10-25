package org.testcontainers.localstack;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalstackTestImages;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class StarterScriptTest {

    @Test
    void starterScriptIsCompleteBeforeTheContainerStarts() {
        AtomicReference<String> script = new AtomicReference<>();

        try (
            LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE) {
                @Override
                protected void containerIsCreated(String containerId) {
                    super.containerIsCreated(containerId);
                    script.set(
                        copyFileFromContainer(
                            "/testcontainers_start.sh",
                            stream -> IOUtils.toString(stream, StandardCharsets.UTF_8)
                        )
                    );
                }
            }
        ) {
            localstack.start();
        }

        // read back while the container was created but not yet started, so the entrypoint could not
        // have executed a partially copied script
        assertThat(script.get()).endsWith("/usr/local/bin/docker-entrypoint.sh\n");
    }
}
