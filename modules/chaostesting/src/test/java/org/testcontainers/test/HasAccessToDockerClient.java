package org.testcontainers.test;

import com.github.dockerjava.api.DockerClient;
import org.testcontainers.DockerClientFactory;

interface HasAccessToDockerClient {

    default DockerClient dockerClient() {
        return DockerClientFactory.instance().client();
    }
}
