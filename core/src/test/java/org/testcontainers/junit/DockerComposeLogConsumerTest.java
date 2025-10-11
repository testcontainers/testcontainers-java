package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DockerComposeLogConsumerTest {

    @Test
    void testLogConsumer() throws TimeoutException {
        WaitingConsumer logConsumer = new WaitingConsumer();
        DockerComposeContainer environment = new DockerComposeContainer(
            DockerImageName.parse("docker/compose:1.29.2"),
            new File("src/test/resources/v2-compose-test.yml")
        )
            .withExposedService("redis_1", 6379)
            .withLogConsumer("redis_1", logConsumer);

        try {
            environment.start();
            logConsumer.waitUntil(
                frame -> {
                    return (
                        frame.getType() == OutputType.STDOUT &&
                        frame.getUtf8String().contains("Ready to accept connections")
                    );
                },
                5,
                TimeUnit.SECONDS
            );
        } finally {
            environment.stop();
        }
    }
}
