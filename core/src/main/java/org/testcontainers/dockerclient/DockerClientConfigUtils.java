package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;

public class DockerClientConfigUtils {
    public static String getDockerHostIpAddress(DockerClientConfig config) {
        switch (config.getUri().getScheme()) {
        case "http":
        case "https":
        case "tcp":
            return config.getUri().getHost();
        case "unix":
            return "localhost";
        default:
            return null;
        }
    }
}
