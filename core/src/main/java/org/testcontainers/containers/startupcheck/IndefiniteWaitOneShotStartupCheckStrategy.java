package org.testcontainers.containers.startupcheck;

import com.google.common.util.concurrent.Uninterruptibles;
import org.testcontainers.controller.ContainerController;

import java.util.concurrent.TimeUnit;

/**
 * Variant of {@link OneShotStartupCheckStrategy} that does not impose a timeout.
 * Intended for situation such as when a long running task forms part of container startup.
 * <p>
 * It has to be assumed that the container will stop of its own accord, either with a success or failure exit code.
 */
public class IndefiniteWaitOneShotStartupCheckStrategy extends OneShotStartupCheckStrategy {
    @Override
    public boolean waitUntilStartupSuccessful(ContainerController containerController, String containerId) {
        while (checkStartupState(containerController, containerId) == StartupStatus.NOT_YET_KNOWN) {
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        return checkStartupState(containerController, containerId) == StartupStatus.SUCCESSFUL;
    }
}
