package org.testcontainers.test;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;

interface HasAccessToDockerClient {

    default DockerClient dockerClient() {
        return DockerClientFactory.instance().client();
    }
}
