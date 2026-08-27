package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent {@link GenericContainer#start()} calls on the same
 * container do not result in {@link GenericContainer#doStart()} being
 * called more than once.
 *
 * <p>A background thread calls {@code container.start()} during class initialization,
 * racing with the extension's {@link Startables#deepStart(Stream)} which also starts
 * the container as part of the {@code @Container} lifecycle.</p>
 */
@Testcontainers(parallel = true)
class ParallelDependsOnTest {
    static {
        // Pre-initialize the Docker client to increase the chance of a race
        // and to emulate a test suite where an earlier test class already
        // triggered the initialization.
        DockerClientFactory.instance().client();
    }

    private static final AtomicInteger doStartCount = new AtomicInteger();

    @Container
    private static final StartCountingContainer container = new StartCountingContainer(
        JUnitJupiterTestImages.HTTPD_IMAGE,
        doStartCount
    );

    static {
        // Race with the extension's Startables.deepStart.
        new Thread(() -> container.start()).start();
    }

    @Test
    void containerShouldBeStartedOnlyOnce() {
        assertThat(container.isRunning()).isTrue();
        assertThat(doStartCount).as("doStart() invocations").hasValue(1);
    }

    private static class StartCountingContainer extends GenericContainer<StartCountingContainer> {

        private final AtomicInteger doStartCount;

        StartCountingContainer(DockerImageName image, AtomicInteger doStartCount) {
            super(image);
            this.doStartCount = doStartCount;
        }

        @Override
        protected void doStart() {
            doStartCount.incrementAndGet();
            super.doStart();
        }
    }
}
