package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.command.EventsResultCallback;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Test that event streaming from the {@link DockerClient} works correctly
 */
public class EventStreamTest {

    /**
     * Test that docker events can be streamed from the client.
     */
    @Test
    public void test() throws IOException, InterruptedException {
        AtomicBoolean received = new AtomicBoolean(false);

        try (
            GenericContainer container = new GenericContainer<>()
                .withCommand("true")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
            String createdAt = container.getContainerInfo().getCreated();
            String finishedAt = container.getCurrentContainerInfo().getState().getFinishedAt();

            // Request all events between startTime and endTime for the container
            try (
                EventsResultCallback response = DockerClientFactory.instance().client().eventsCmd()
                    .withContainerFilter(container.getContainerId())
                    .withEventFilter("create")
                    .withSince(Instant.parse(createdAt).getEpochSecond() + "")
                    .withUntil(Instant.parse(finishedAt).getEpochSecond() + "")
                    .exec(new EventsResultCallback() {
                        @Override
                        public void onNext(@NotNull Event event) {
                            // Check that a create event for the container is received
                            if (event.getId().equals(container.getContainerId()) && event.getStatus().equals("create")) {
                                received.set(true);
                            }
                        }
                    })
            ) {
                response.awaitCompletion();
            }
        }

        assertTrue("Events has been captured", received.get());
    }

}
