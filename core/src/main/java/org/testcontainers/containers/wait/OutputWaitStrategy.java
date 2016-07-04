package org.testcontainers.containers.wait;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
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
        Preconditions.checkArgument(this.container == null || container == this.container, "Strategy can not be used for multiple containers");
        if (this.container == null) {
            this.container = container;
            this.readyStateOutputConsumer = new ReadyStateOutputConsumer(this.container, this.readyCheckFunction);
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
        Boolean apply(Container container, String outputFrame) throws Exception;
    }

    private class ReadyStateOutputConsumer implements Consumer<OutputFrame> {
        private GenericContainer container;
        private ContainerReadyCheckFunction readyCheckFunction;

        private boolean ready = false;

        ReadyStateOutputConsumer(GenericContainer container, ContainerReadyCheckFunction readyCheckFunction) {
            this.container = container;
            this.readyCheckFunction = readyCheckFunction;
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            if (!ready) {
                try {
                    if (outputFrame.getBytes() != null) {
                        ready = readyCheckFunction.apply(container, new String(outputFrame.getBytes(), Charsets.UTF_8));
                    }
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

