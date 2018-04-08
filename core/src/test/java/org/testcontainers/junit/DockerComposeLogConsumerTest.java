package org.testcontainers.junit;

import org.junit.Test;
import org.junit.runner.Description;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.WaitingConsumer;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class DockerComposeLogConsumerTest {

    @Test
    public void testLogConsumer() throws TimeoutException {
        WaitingConsumer logConsumer = new WaitingConsumer();
        DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/v2-compose-test.yml"))
            .withExposedService("redis_1", 6379)
            .withLogConsumer("redis_1", logConsumer);

        try {
            environment.starting(Description.EMPTY);
            logConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("Ready to accept connections"), 5, TimeUnit.SECONDS);
        } finally {
            environment.finished(Description.EMPTY);
        }
    }
}
