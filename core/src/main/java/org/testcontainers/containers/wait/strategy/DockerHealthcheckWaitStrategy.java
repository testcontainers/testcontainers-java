package org.testcontainers.containers.wait.strategy;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;

import java.util.concurrent.TimeUnit;

/**
 * Wait strategy leveraging Docker's built-in healthcheck mechanism.
 *
 * @see <a href="https://docs.docker.com/engine/reference/builder/#healthcheck">https://docs.docker.com/engine/reference/builder/#healthcheck</a>
 */
public class DockerHealthcheckWaitStrategy extends AbstractWaitStrategy {

    @Override
    protected void waitUntilReady() {

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, waitStrategyTarget::isHealthy);
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container to become healthy");
        }
    }
}
