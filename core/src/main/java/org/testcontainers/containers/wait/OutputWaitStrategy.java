package org.testcontainers.containers.wait;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.util.function.Consumer;

/**
 * Created by qoomon on 21/06/16.
 */
public class OutputWaitStrategy extends GenericWaitStrategy<OutputWaitStrategy> {

    private final ContainerReadyCheckFunction readyCheckFunction;

    private GenericContainer container;
    private ReadyStateOutputConsumer readyStateOutputConsumer;


    public OutputWaitStrategy(ContainerReadyCheckFunction readyCheckFunction) {
        super("ready statement on output stream");
        this.readyCheckFunction = readyCheckFunction;
    }


    @Override
    public void waitUntilReady(GenericContainer container) {
        Preconditions.checkState(this.container == null || this.container == container, "this strategy instance can only be used by ONE container instance");
        if (this.container == null) {
            this.container = container;
            this.readyStateOutputConsumer = new ReadyStateOutputConsumer(this.container, logger(this.container), this.readyCheckFunction);
            this.container.followOutput(this.readyStateOutputConsumer);
        }
        super.waitUntilReady(this.container);
    }

    @Override
    protected boolean isReady(GenericContainer container) throws Exception {
        return readyStateOutputConsumer.isReady();
    }

    @FunctionalInterface
    public interface ContainerReadyCheckFunction {
        Boolean apply(Container container, Logger containerLogger, String outputFrame) throws Exception;
    }

    private class ReadyStateOutputConsumer implements Consumer<OutputFrame> {
        private GenericContainer container;
        private Logger containerLogger;
        private ContainerReadyCheckFunction readyCheckFunction;

        private boolean ready = false;

        ReadyStateOutputConsumer(GenericContainer container, Logger containerLogger, ContainerReadyCheckFunction readyCheckFunction) {
            this.container = container;
            this.containerLogger = containerLogger;
            this.readyCheckFunction = readyCheckFunction;
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            if (!ready) {
                try {
                    ready = readyCheckFunction.apply(container, containerLogger, outputFrame.getUtf8String());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public boolean isReady() {
            return ready;
        }

    }
}

