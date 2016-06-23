package org.testcontainers.containers.wait;

import com.google.common.base.Charsets;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Created by qoomon on 21/06/16.
 */
public class OutputWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private final String description;
    private final Predicate<OutputFrame> predicate;


    public OutputWaitStrategy(String description, Predicate<OutputFrame> predicate) {
        this.description = description;
        this.predicate = predicate;
    }

    @Override
    protected void waitUntilReady() {

        logger().info("Waiting up to {} seconds for output {}", startupTimeout.getSeconds(), description);

        try {
            WaitingConsumer outputConsumer = new WaitingConsumer();
            container.followOutput(outputConsumer);
            outputConsumer.waitUntil(predicate, startupTimeout.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            /**  Needs to be done due to stuck caused by {@link GenericContainer#followOutput} */
            container.stop();
            throw new ContainerLaunchException("missing output " + description, e);
        }
    }
}

