package org.testcontainers.docker;

import com.github.dockerjava.api.DockerClient;
import lombok.ToString;
import lombok.experimental.Delegate;

@ToString
enum LazyDockerClient implements DockerClient {

    INSTANCE;

    @Delegate
    final DockerClient getDockerClient() {
        return DockerClientFactory.instance().client();
    }
}
