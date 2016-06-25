package org.testcontainers.containers.wait;

import com.google.common.base.Preconditions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Created by qoomon on 21/06/16.
 */
public class OutputWaitStrategy extends GenericWaitStrategy<OutputWaitStrategy> {

    WaitingConsumer outputConsumer = new WaitingConsumer();

    private final Predicate<OutputFrame> predicate;

    private GenericContainer container;


    public OutputWaitStrategy(Predicate<OutputFrame> predicate) {
        super("ready statement on output stream");
        this.predicate = predicate;
    }


    @Override
    public void waitUntilReady(GenericContainer container) {
        Preconditions.checkState(this.container == null || this.container == container, "this strategy can only be used by ONE container instance");
        if (this.container == null) {
            this.container = container;
            container.followOutput(outputConsumer);
        }
        super.waitUntilReady(container);
    }


    @Override
    protected boolean isReady(GenericContainer container) throws Exception {
        logger(container).info("Waiting for " + description());
        outputConsumer.waitUntil(predicate, startupTimeout.getSeconds(), TimeUnit.SECONDS);
        return true;
    }
}

