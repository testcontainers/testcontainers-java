package org.testcontainers.containers.wait;

import com.github.dockerjava.api.model.PortBinding;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * Created by qoomon on 25.06.16.
 * Generic Wait Strategy
 */
public abstract class GenericWaitStrategy<SELF extends GenericWaitStrategy<SELF>> implements WaitStrategy {


    private final String description;

    protected Duration startupTimeout = Duration.ofSeconds(60);

    protected Duration readyCheckDelay = Duration.ofSeconds(1);

    public GenericWaitStrategy(String description) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(description), "description must be not null nor empty.");
        this.description = description;
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    @Override
    public SELF withStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = Preconditions.checkNotNull(startupTimeout, "startupTimeout must not be null.");
        return this.self();
    }


    /**
     * Try to find primary container port.
     * <br>
     * <pre>
     * 1st priority: mapped port of first exposed port {@link GenericContainer#getExposedPorts()}
     * 2nd priority: host port of first port binding {@link GenericContainer#getPortBindings()}
     * </pre>
     *
     * @param container the container
     * @return primary mapped container port if any specified
     */
    protected Optional<Integer> getPrimaryMappedContainerPort(GenericContainer<?> container) {
        List<Integer> exposedPorts = container.getExposedPorts();
        if (!exposedPorts.isEmpty()) {
            return Optional.of(container.getMappedPort(exposedPorts.get(0)));
        }

        List<String> portBindings = container.getPortBindings();
        if (!portBindings.isEmpty()) {
            return Optional.of(container.getMappedPort(PortBinding.parse(portBindings.get(0)).getExposedPort().getPort()));
        }

        return Optional.empty();
    }


    @Override
    public void waitUntilReady(GenericContainer container) {

        String strategySimpleName = getClass().getSimpleName();
        if(strategySimpleName.isEmpty()){
            strategySimpleName = "<Anonymous>";
        }
        container.logger().info("Use wait strategy " + getClass().getSimpleName() + " [" +  getClass().getName() + "]");
        container.logger().info("Waiting up to {} seconds for {}", startupTimeout.getSeconds(), description);

        try {
            retryUntilReady(startupTimeout, readyCheckDelay,() -> {
                container.logger().debug("Check container ready state");
                return isReady(container);
            });
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    protected void retryUntilReady(Duration retryTimeout, Duration retryDelay, Callable<Boolean> isReadyCheck) throws TimeoutException {
        long waitStartTime = System.currentTimeMillis();
        long waitEndTime = waitStartTime + retryTimeout.toMillis();
        long nextExecution = waitStartTime;
        boolean isReady = false;

        while (!isReady) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > waitEndTime || nextExecution > waitEndTime) {
                throw new TimeoutException("Retry timeout!");
            }
            if (currentTime < nextExecution) {
                try {
                    Thread.sleep(nextExecution - currentTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                isReady = isReadyCheck.call();
            } catch (Exception e) {
                // ignore
            }

            // prepare for next iteration
            nextExecution += retryDelay.toMillis();
        }
    }


    /**
     * Container ready check
     *
     * @param container container to wait for
     * @return true if ready, false else
     * @throws Exception any exception is treated as not ready
     */
    protected abstract boolean isReady(GenericContainer container) throws Exception;


    public String description() {
        return description;
    }
}