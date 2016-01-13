package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;

/**
 * Created by rnorth on 13/01/2016.
 */
public class DockerClientConfigUtils {
    public static String getDockerHostIpAddress(DockerClientConfig config) {
        return config.getUri().getHost();
    }
}
