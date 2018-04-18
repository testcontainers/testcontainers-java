package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.DockerClientFactory;

import java.io.File;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class DockerClientConfigUtils {

    // See https://github.com/docker/docker/blob/a9fa38b1edf30b23cae3eade0be48b3d4b1de14b/daemon/initlayer/setup_unix.go#L25
    public static final boolean IN_A_CONTAINER = new File("/.dockerenv").exists();

    @Deprecated
    @Getter(lazy = true)
    private static final Optional<String> detectedDockerHostIp = IN_A_CONTAINER ? getDefaultGateway() : Optional.empty();

    @Getter(lazy = true)
    private static final Optional<String> defaultGateway = Optional
            .ofNullable(DockerClientFactory.instance().runInsideDocker(
                    cmd -> cmd.withCmd("sh", "-c", "ip route|awk '/default/ { print $3 }'"),
                    (client, id) -> {
                        try {
                            LogToStringContainerCallback loggingCallback = new LogToStringContainerCallback();
                            client.logContainerCmd(id).withStdOut(true)
                                                      .withFollowStream(true)
                                                      .exec(loggingCallback)
                                                      .awaitStarted();
                            loggingCallback.awaitCompletion(3, SECONDS);
                            return loggingCallback.toString();
                        } catch (Exception e) {
                            log.warn("Can't parse the default gateway IP", e);
                            return null;
                        }
                    }
            ))
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank);



    public static String getDockerHostIpAddress(DockerClientConfig config) {
        switch (config.getDockerHost().getScheme()) {
            case "http":
            case "https":
            case "tcp":
                return config.getDockerHost().getHost();
            case "unix":
                if (IN_A_CONTAINER) {
                    return getDefaultGateway().orElse("localhost");
                }
                return "localhost";
            default:
                return null;
        }
    }
}
