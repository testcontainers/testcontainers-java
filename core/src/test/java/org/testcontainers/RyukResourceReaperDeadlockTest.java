package org.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.ResourceReaper;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RyukResourceReaperDeadlockTest {

    @Test
    void shouldNotHoldReaperLockWhenCallingDockerClientFactory() {
        ResourceReaper reaper = ResourceReaper.instance();
        AtomicBoolean heldLock = new AtomicBoolean(true);

        DockerClientFactory dockerClientFactory = DockerClientFactory.instance();
        RuntimeException simulatedFailure = new RuntimeException("Simulated failure to intercept call") {
            @Override
            public synchronized Throwable fillInStackTrace() {
                heldLock.set(Thread.holdsLock(reaper));
                return this;
            }
        };

        dockerClientFactory.cachedClientFailure = simulatedFailure;
        try {
            assertThatThrownBy(() -> reaper.registerLabelsFilterForCleanup(Collections.singletonMap("k", "v")))
                .isSameAs(simulatedFailure);

            assertThat(heldLock.get())
                .as("RyukResourceReaper monitor must NOT be held when calling DockerClientFactory to prevent deadlocks")
                .isFalse();
        } finally {
            dockerClientFactory.cachedClientFailure = null;
        }
    }

    @Test
    void concurrentCallersShouldNotDeadlockUnderContention() throws InterruptedException {
        ResourceReaper reaper = ResourceReaper.instance();
        DockerClientFactory dockerClientFactory = DockerClientFactory.instance();
        RuntimeException simulatedFailure = new RuntimeException("Simulated client error");

        dockerClientFactory.cachedClientFailure = simulatedFailure;
        try {
            int threadCount = 8;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        startLatch.await(2, TimeUnit.SECONDS);
                        if (id % 2 == 0) {
                            reaper.registerLabelsFilterForCleanup(Collections.singletonMap("test", "val" + id));
                        } else {
                            reaper.getLabels();
                        }
                    } catch (Throwable ignored) {} finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertThat(finished)
                .as("Concurrent calls to RyukResourceReaper must finish within timeout and never deadlock")
                .isTrue();
        } finally {
            dockerClientFactory.cachedClientFailure = null;
        }
    }
}
