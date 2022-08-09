package org.testcontainers.junit;

import org.junit.Test;
import org.testcontainers.containers.ContainerFetchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Created by rnorth on 20/03/2016.
 */
public class NonExistentImagePullTest {

    @Test(timeout = 60_000L)
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
