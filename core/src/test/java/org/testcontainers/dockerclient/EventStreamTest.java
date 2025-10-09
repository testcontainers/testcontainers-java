package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.command.EventsResultCallback;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test that event streaming from the {@link DockerClient} works correctly
 */
@Timeout(10)
class EventStreamTest {

    /**
     * Test that docker events can be streamed from the client.
     */
    @Test
    void test() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("true")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
            String createdAt = container.getContainerInfo().getCreated();

            // Request all events between startTime and endTime for the container
            try (
                EventsResultCallback response = DockerClientFactory
                    .instance()
                    .client()
                    .eventsCmd()
                    .withContainerFilter(container.getContainerId())
                    .withEventFilter("create")
                    .withSince(Instant.parse(createdAt).getEpochSecond() + "")
                    .exec(
                        new EventsResultCallback() {
                            @Override
                            public void onNext(@NotNull Event event) {
                                // Check that a create event for the container is received
                                if (
                                    event.getId().equals(container.getContainerId()) &&
                                    event.getStatus().equals("create")
                                ) {
                                    latch.countDown();
                                }
                            }
                        }
                    )
            ) {
                response.awaitStarted(5, TimeUnit.SECONDS);
                latch.await(5, TimeUnit.SECONDS);
            }
        }
    }
}
