package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;

public class DockerClientConfigUtils {
    public static String getDockerHostIpAddress(DockerClientConfig config) {
        switch (config.getDockerHost().getScheme()) {
        case "http":
        case "https":
        case "tcp":
            return config.getDockerHost().getHost();
        case "unix":
            return "localhost";
        default:
            return null;
        }
    }
}
