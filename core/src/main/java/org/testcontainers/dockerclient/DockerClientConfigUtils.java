package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;

public class DockerClientConfigUtils {
    public static String getDockerHostIpAddress(DockerClientConfig config) {
        return config.getUri().getHost();
    }
}
