package org.testcontainers.containers.wait.strategy;

import org.awaitility.core.ConditionTimeoutException;
import org.testcontainers.containers.ContainerLaunchException;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Wait strategy leveraging Docker's built-in healthcheck mechanism.
 *
 * @see <a href="https://docs.docker.com/engine/reference/builder/#healthcheck">https://docs.docker.com/engine/reference/builder/#healthcheck</a>
 */
public class DockerHealthcheckWaitStrategy extends AbstractWaitStrategy {

    @Override
    protected void waitUntilReady() {
        try {
            await().atMost(startupTimeout.getSeconds(), TimeUnit.SECONDS).until(waitStrategyTarget::isHealthy);
        } catch (ConditionTimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container to become healthy");
        }
    }
}
