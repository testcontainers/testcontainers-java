package org.testcontainers;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.io.IOException;

/**
 * TODO: Javadocs
 */
@Slf4j
public class DebugSlowMachineTest {

    @BeforeClass
    public static void prepare() {
        DockerClientFactory.instance().client();
    }

    @Test
    public void timeExec() throws IOException, InterruptedException {
        final GenericContainer container = new GenericContainer<>("alpine:3.5")
                .withCommand("top");
        container.start();

        final String stdout = container.execInContainer("date").getStdout();
    }

    @Test
    public void timeExecWithoutStdout() throws IOException, InterruptedException {
        final GenericContainer container = new GenericContainer<>("alpine:3.5")
                .withCommand("top");
        container.start();

        container.execInContainer("date");
    }

    @Test
    public void timeTail() throws IOException, InterruptedException {
        final GenericContainer container = new GenericContainer<>("alpine:3.5")
                .withCommand("ls -al")
                .withLogConsumer(new Slf4jLogConsumer(log))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy());
        container.start();
    }
}
