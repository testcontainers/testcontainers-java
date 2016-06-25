package org.testcontainers.containers.wait;

import org.slf4j.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;


/**
 * Created by qoomon on 21/06/16.
 */
public class SimpleWaitStrategy extends GenericWaitStrategy<SimpleWaitStrategy> {

    private final ContainerReadyCheckFunction readyFunction;


    public SimpleWaitStrategy(String description, ContainerReadyCheckFunction readyFunction) {
        super(description);
        this.readyFunction = readyFunction;
    }

    @Override
    protected boolean isReady(GenericContainer container) throws Exception {
        return readyFunction.apply(container, logger(container));
    }


    @FunctionalInterface
    public interface ContainerReadyCheckFunction {
        Boolean apply(Container container, Logger containerLogger) throws Exception;
    }
}
