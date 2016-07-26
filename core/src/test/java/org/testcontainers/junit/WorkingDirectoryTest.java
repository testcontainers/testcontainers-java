package org.testcontainers.junit;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.util.function.Consumer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Created by rnorth on 26/07/2016.
 */
public class WorkingDirectoryTest {

    private static WaitingConsumer waitingConsumer = new WaitingConsumer();
    private static ToStringConsumer toStringConsumer = new ToStringConsumer();
    private static Consumer<OutputFrame> compositeConsumer = waitingConsumer.andThen(toStringConsumer);

    @ClassRule
    public static GenericContainer container = new GenericContainer("alpine:3.2")
            .withWorkingDirectory("/etc")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
            .withCommand("ls", "-al")
            .withLogConsumer(compositeConsumer);

    @Test
    public void checkOutput() {
        String listing = toStringConsumer.toUtf8String();

        assertTrue("Directory listing contains expected /etc content", listing.contains("hostname"));
        assertTrue("Directory listing contains expected /etc content", listing.contains("init.d"));
        assertTrue("Directory listing contains expected /etc content", listing.contains("passwd"));
    }

}
