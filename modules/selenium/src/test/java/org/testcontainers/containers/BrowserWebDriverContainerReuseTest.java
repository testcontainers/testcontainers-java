package org.testcontainers.containers;

import com.github.dockerjava.api.model.Bind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserWebDriverContainerReuseTest {

    private static final DockerImageName CHROME_IMAGE = DockerImageName.parse("selenium/standalone-chrome:4.13.0");

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void configureDoesNotAddDuplicateShmBindOnReuse() {
        BrowserWebDriverContainer<?> container = new BrowserWebDriverContainer<>(CHROME_IMAGE);

        // configure() runs on every start(), so a reused container that is started
        // more than once must not accumulate duplicate /dev/shm binds (see #11941).
        container.configure();
        container.configure();

        long shmBinds = container
            .getBinds()
            .stream()
            .map(Bind::getVolume)
            .filter(volume -> "/dev/shm".equals(volume.getPath()))
            .count();

        assertThat(shmBinds).isEqualTo(1);
    }
}
