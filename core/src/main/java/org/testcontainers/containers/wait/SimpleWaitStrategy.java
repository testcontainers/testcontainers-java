package org.testcontainers.containers.wait;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.*;

/**
 * Created by qoomon on 21/06/16.
 */
public class SimpleWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private final String description;
    private final ContainerReadyCheckFunction readyFunction;


    public SimpleWaitStrategy(String description, ContainerReadyCheckFunction readyFunction) {
        this.description = description;
        this.readyFunction = readyFunction;
    }

    @Override
    protected void waitUntilReady() {

        logger().info("Waiting up to {} seconds for {}", startupTimeout.getSeconds(), description);

        retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
            getRateLimiter().doWhenReady(() -> {
                Boolean ready;
                try {
                    ready = readyFunction.apply(container);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!ready) {
                    throw new RuntimeException("Container not ready yet!");
                }
            });
            return true;
        });
    }

    @FunctionalInterface
    public interface ContainerReadyCheckFunction {

        Boolean apply(Container container) throws Exception;
    }
}
