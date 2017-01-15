package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.DockerClientFactory;

import java.io.File;
import java.util.Optional;

@Slf4j
public class DockerClientConfigUtils {

    // See https://github.com/docker/docker/blob/a9fa38b1edf30b23cae3eade0be48b3d4b1de14b/daemon/initlayer/setup_unix.go#L25
    public static final boolean IN_A_CONTAINER = new File("/.dockerenv").exists();

    @Getter(lazy = true)
    private static final Optional<String> detectedDockerHostIp = Optional
            .of(IN_A_CONTAINER)
            .filter(it -> it)
            .map(file -> DockerClientFactory.instance().runInsideDocker(
                    cmd -> cmd.withCmd("sh", "-c", "ip route|awk '/default/ { print $3 }'"),
                    (client, id) -> {
                        try {
                            return client.logContainerCmd(id)
                                    .withStdOut(true)
                                    .exec(new LogToStringContainerCallback())
                                    .toString();
                        } catch (Exception e) {
                            log.warn("Can't parse the default gateway IP", e);
                            return null;
                        }
                    }
            ))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank);

    public static String getDockerHostIpAddress(DockerClientConfig config) {
        return getDetectedDockerHostIp().orElseGet(() -> {
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
        });
    }
}
