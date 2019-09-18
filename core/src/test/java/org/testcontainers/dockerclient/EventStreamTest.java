package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.command.EventsResultCallback;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test that event streaming from the {@link DockerClient} works correctly
 */
public class EventStreamTest {

    /**
     * Test that docker events can be streamed from the client.
     */
    @Test
    public void test() throws IOException, InterruptedException {
        DockerClient client = DockerClientFactory.instance().client();
        Instant startTime = Instant.now();
        final AtomicBoolean received = new AtomicBoolean(false);

        // Start a one-shot Container
        try (
            GenericContainer container = new GenericContainer<>()
                .withCommand("/bin/sh", "-c", "sleep 0")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {

            container.start();
            Instant endTime = Instant.now();

            // Request all events between startTime and endTime for the container
            try (EventsResultCallback response = client.eventsCmd()
                .withContainerFilter(container.getContainerId())
                .withEventFilter("create")
                .withSince(Long.toString(startTime.getEpochSecond()))
                .withUntil(Long.toString(endTime.getEpochSecond()))
                .exec(new EventsResultCallback() {
                    @Override
                    public void onNext(@NotNull Event event) {
                        // Check that a create event for the container is received
                        if (event.getId().equals(container.getContainerId()) && event.getStatus().equals("create")) {
                            received.set(true);
                        }
                    }
                })) {

                response.awaitCompletion();
            }

        }

        VisibleAssertions.assertTrue("Events has been captured", received.get());
    }

}
