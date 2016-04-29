package org.testcontainers.junit;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.traits.ExposedPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class CustomTraitTest {

    @Rule
    public GenericContainer container = new GenericContainer<>()
            .with((container1, createContainerCmd) -> createContainerCmd.withHostName("some.domain.com"))
            .with(new ExposedPort<>(80))
            .withCommand("/bin/sh", "-c", "while true; do echo \"`hostname`\" | nc -l -p 80; done");

    @Test
    public void customTraitTest() throws IOException {
        BufferedReader br = Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            Socket socket = new Socket(container.getContainerIpAddress(), container.getMappedPort(80));
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        });

        try  {
            assertEquals("A container built with custom trait returns configured hostname",
                    br.readLine(), "some.domain.com");
        } finally {
            IOUtils.closeQuietly(br);
        }
    }
}
