package org.testcontainers.containers.wait;

import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.PortBinding;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerLoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by qoomon on 25.06.16.
 * Generic Wait Strategy
 */
public abstract class GenericWaitStrategy<SELF extends GenericWaitStrategy<SELF>> implements WaitStrategy {


    private static final RateLimiter RATE_LIMITER = RateLimiterBuilder.newBuilder()
            .withRate(1, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    private final String description;

    protected Duration startupTimeout = Duration.ofSeconds(60);

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

    protected Logger logger(GenericContainer container) {
        return DockerLoggerFactory.getLogger(container.getDockerImageName());
    }

    protected int getDefaultPort(GenericContainer<?> container) {
        List<Integer> exposedPorts = container.getExposedPorts();
        if (!exposedPorts.isEmpty()) {
            return container.getMappedPort(exposedPorts.get(0));
        } else {
            List<String> portBindings = container.getPortBindings();
            if (!portBindings.isEmpty()) {
                return PortBinding.parse(portBindings.get(0)).getBinding().getHostPort();
            } else {
                throw new WaitStrategyException("Could not find any reachable port");
            }
        }
    }

    @Override
    public void waitUntilReady(GenericContainer container) {

        logger(container).info("Waiting up to {} seconds for {}", startupTimeout.getSeconds(), description);

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> RATE_LIMITER.getWhenReady(() -> {
                try {
                    return isReady(container);
                } catch (Exception e) {
                    throw new RuntimeException(description + " failed!", e);
                }
            }));
        } catch (Exception e) {
            container.stop();
            throw new WaitStrategyException("Timed out waiting for container to be ready. Waited for " + description + ".", e);
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