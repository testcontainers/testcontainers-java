package org.testcontainers.containers.startupcheck;

import com.github.dockerjava.api.DockerClient;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

/**
 * Variant of {@link OneShotStartupCheckStrategy} that does not impose a timeout.
 * Intended for situation such as when a long running task forms part of container startup.
 * <p>
 * It has to be assumed that the container will stop of its own accord, either with a success or failure exit code.
 */
public class IndefiniteWaitOneShotStartupCheckStrategy extends OneShotStartupCheckStrategy {
    @Override
    public boolean waitUntilStartupSuccessful(DockerClient dockerClient, String containerId) {
        while (checkStartupState(dockerClient, containerId) == StartupStatus.NOT_YET_KNOWN) {
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        return checkStartupState(dockerClient, containerId) == StartupStatus.SUCCESSFUL;
    }
}
