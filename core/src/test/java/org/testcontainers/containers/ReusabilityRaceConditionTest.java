package org.testcontainers.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import com.github.dockerjava.core.command.ListContainersCmdImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.TestImages;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the check-then-act race in {@link GenericContainer#findContainerForReuse(String)} that
 * used to be marked with a "// TODO locking" comment: two containers with the identical reuse
 * hash, started concurrently, could both miss the reuse lookup and both end up creating a new
 * container, silently defeating the purpose of {@code withReuse(true)}.
 *
 * <p>{@link GenericContainer#tryStart()} now guards the "check reuse, else create" sequence with
 * a per-hash JVM-local lock, which is what this test now verifies: exactly one real container is
 * created for two same-JVM concurrent starts with an identical hash, and the second one reuses
 * the first. The lock only protects same-JVM races; cross-process races are covered separately by
 * {@code GenericContainer#createOrJoinReusableContainer}'s name-conflict handling, exercised in
 * {@link ReusabilityNameConflictTest}.
 */
class ReusabilityRaceConditionTest extends ReusabilityUnitTests.AbstractReusabilityTest {

    // Simulates the Docker daemon's container list, updated only once a "create" call completes -
    // exactly like the real docker daemon, there is no lock held between the "list" read and the "create" write.
    private final List<String> runningContainerIds = new CopyOnWriteArrayList<>();

    @Test
    void concurrentStartsWithIdenticalHashCreateExactlyOneContainer() throws InterruptedException {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        when(client.listContainersCmd())
            .then(invocation -> {
                ListContainersCmd.Exec exec = command -> {
                    return new ObjectMapper()
                        .convertValue(
                            runningContainerIds.stream().map(id -> singletonIdMap(id)).collect(Collectors.toList()),
                            new TypeReference<List<Container>>() {}
                        );
                };
                return new ListContainersCmdImpl(exec);
            });

        when(client.createContainerCmd(any()))
            .then(invocation -> {
                CreateContainerCmd.Exec exec = command -> {
                    // Simulate real-world latency of an actual "docker create" round trip,
                    // widening the window between the reuse lookup and the container becoming visible.
                    sleepQuietly(300);
                    String newId = "created-" + java.util.UUID.randomUUID();
                    runningContainerIds.add(newId);
                    CreateContainerResponse response = new CreateContainerResponse();
                    response.setId(newId);
                    return response;
                };
                return new CreateContainerCmdImpl(exec, null, "image:latest");
            });

        when(client.startContainerCmd(any())).then(inv -> startContainerAnswer().answer(inv));
        when(client.inspectContainerCmd(any())).then(inv -> inspectContainerAnswer().answer(inv));

        GenericContainer<?> containerA = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));
        GenericContainer<?> containerB = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));

        CountDownLatch bothReady = new CountDownLatch(2);
        Runnable startAndCountDown = () -> {
            bothReady.countDown();
            try {
                bothReady.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Thread t1 = new Thread(() -> {
            startAndCountDown.run();
            containerA.start();
        });
        Thread t2 = new Thread(() -> {
            startAndCountDown.run();
            containerB.start();
        });

        t1.start();
        t2.start();
        t1.join(10_000);
        t2.join(10_000);

        // Both containers were configured identically (same image, same reuse hash), and reuse
        // was requested for both. With the per-hash JVM-local lock in tryStart(), the two threads
        // are serialized around "check reuse, else create": whichever thread wins the lock first
        // creates the real container, and the other thread's lookup (also inside the lock) then
        // finds it and reuses it, so exactly one real container is ever created.
        assertThat(runningContainerIds).as("number of containers actually created").hasSize(1);
        assertThat(containerA.getContainerId()).isEqualTo(containerB.getContainerId());
    }

    @Test
    void twoOrMoreThreadsRacingStillCreateExactlyOneContainer() throws InterruptedException {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        when(client.listContainersCmd())
            .then(invocation -> {
                ListContainersCmd.Exec exec = command -> {
                    return new ObjectMapper()
                        .convertValue(
                            runningContainerIds.stream().map(id -> singletonIdMap(id)).collect(Collectors.toList()),
                            new TypeReference<List<Container>>() {}
                        );
                };
                return new ListContainersCmdImpl(exec);
            });

        when(client.createContainerCmd(any()))
            .then(invocation -> {
                CreateContainerCmd.Exec exec = command -> {
                    sleepQuietly(100);
                    String newId = "created-" + java.util.UUID.randomUUID();
                    runningContainerIds.add(newId);
                    CreateContainerResponse response = new CreateContainerResponse();
                    response.setId(newId);
                    return response;
                };
                return new CreateContainerCmdImpl(exec, null, "image:latest");
            });

        when(client.startContainerCmd(any())).then(inv -> startContainerAnswer().answer(inv));
        when(client.inspectContainerCmd(any())).then(inv -> inspectContainerAnswer().answer(inv));

        int threadCount = 5;
        CountDownLatch allReady = new CountDownLatch(threadCount);
        List<GenericContainer<?>> containers = new CopyOnWriteArrayList<>();
        List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            GenericContainer<?> container = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));
            containers.add(container);
            Thread t = new Thread(() -> {
                allReady.countDown();
                try {
                    allReady.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                container.start();
            });
            threads.add(t);
        }

        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join(10_000);
        }

        assertThat(runningContainerIds).as("number of containers actually created").hasSize(1);
        assertThat(containers.stream().map(GenericContainer::getContainerId).collect(Collectors.toSet())).hasSize(1);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static java.util.Map<String, String> singletonIdMap(String id) {
        return java.util.Collections.singletonMap("Id", id);
    }
}
