package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Created by rnorth on 20/03/2016.
 */
public class NonExistentImagePullTest {

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void pullingNonExistentImageFailsGracefully() {
        assertThat(
            catchThrowable(() -> {
                new GenericContainer<>(DockerImageName.parse("testcontainers/nonexistent:latest")).getDockerImageName();
            })
        )
            .as("Pulling a nonexistent container will cause an exception to be thrown")
            .isInstanceOf(ContainerFetchException.class);
    }
}
