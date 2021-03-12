package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
class DelegatingDockerClient implements DockerClient {

    @Delegate
    private final DockerClient dockerClient;
}
