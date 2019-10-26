package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import lombok.experimental.Delegate;
import org.testcontainers.DockerClientFactory;

enum LazyDockerClient implements DockerClient {

    INSTANCE;

    @Delegate
    final DockerClient getDockerClient() {
        return DockerClientFactory.instance().client();
    }
}
