package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import com.github.dockerjava.core.command.InspectContainerCmdImpl;
import com.github.dockerjava.core.command.ListContainersCmdImpl;
import com.github.dockerjava.core.command.StartContainerCmdImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testcontainers.TestImages;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent calls to {@link GenericContainer#start()} on a single container instance
 * only ever create/start one underlying Docker container.
 *
 * <p>Uses a mocked {@link DockerClient} (like {@code ReusabilityUnitTests}) so the test is fully
 * deterministic and does not require a Docker environment. A latch inside the {@code createContainerCmd}
 * mock forces the racing threads to interleave inside the critical section, so the race is reproduced
 * reliably rather than depending on thread scheduling.
 */
@ExtendWith(MockTestcontainersConfigurationExtension.class)
class GenericContainerConcurrentStartTest {

    private static final int THREADS = 4;

    private final DockerClient client = Mockito.mock(DockerClient.class);

    private final AtomicInteger createInvocations = new AtomicInteger(0);

    private final AtomicInteger startInvocations = new AtomicInteger(0);

    // Latch that holds every thread inside the create command until all racing threads have arrived,
    // guaranteeing they are all past the `containerId == null` guard at the same time.
    private final CountDownLatch insideCreate = new CountDownLatch(THREADS);

    @Test
    void concurrentStartCreatesSingleContainer() throws Exception {
        GenericContainer<?> container = makeTestable(new GenericContainer<>(TestImages.TINY_IMAGE));

        String containerId = UUID.randomUUID().toString();
        Mockito.when(client.createContainerCmd(Mockito.any())).then(createContainerAnswer(containerId));
        Mockito.when(client.listContainersCmd()).then(listContainersAnswer());
        Mockito.when(client.startContainerCmd(Mockito.anyString())).then(startContainerAnswer());
        Mockito.when(client.inspectContainerCmd(Mockito.anyString())).then(inspectContainerAnswer());

        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    container.start();
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(done.await(30, TimeUnit.SECONDS)).as("all start() calls completed").isTrue();
        executor.shutdownNow();

        assertThat(failures).as("no start() call threw").isEmpty();
        assertThat(createInvocations.get()).as("only one container was created").isEqualTo(1);
        assertThat(startInvocations.get()).as("only one container was started").isEqualTo(1);
    }

    private <T extends GenericContainer<?>> T makeTestable(T container) {
        container.dockerClient = client;
        container.withNetworkMode("none"); // to disable the port forwarding
        container.withStartupCheckStrategy(
            new StartupCheckStrategy() {
                @Override
                public boolean waitUntilStartupSuccessful(DockerClient dockerClient, String containerId) {
                    return true;
                }

                @Override
                public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
                    return StartupStatus.SUCCESSFUL;
                }
            }
        );
        container.waitingFor(
            new AbstractWaitStrategy() {
                @Override
                protected void waitUntilReady() {}
            }
        );
        return container;
    }

    private Answer<CreateContainerCmd> createContainerAnswer(String containerId) {
        return invocation -> {
            CreateContainerCmd.Exec exec = command -> {
                createInvocations.incrementAndGet();
                // Force any racing threads to be inside the critical section simultaneously, so that a
                // broken (unsynchronized) start() reliably creates more than one container. When start()
                // is correctly synchronized only one thread ever reaches this point; the bounded await
                // then simply elapses without affecting the assertions.
                insideCreate.countDown();
                try {
                    insideCreate.await(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                CreateContainerResponse response = new CreateContainerResponse();
                response.setId(containerId);
                return response;
            };
            return new CreateContainerCmdImpl(exec, null, "image:latest");
        };
    }

    private Answer<StartContainerCmd> startContainerAnswer() {
        return invocation -> {
            StartContainerCmd.Exec exec = command -> {
                startInvocations.incrementAndGet();
                return null;
            };
            return new StartContainerCmdImpl(exec, invocation.getArgument(0));
        };
    }

    private Answer<ListContainersCmd> listContainersAnswer() {
        return invocation -> {
            ListContainersCmd.Exec exec = command -> Collections.emptyList();
            return new ListContainersCmdImpl(exec);
        };
    }

    private Answer<InspectContainerCmd> inspectContainerAnswer() {
        return invocation -> {
            InspectContainerCmd.Exec exec = command -> {
                InspectContainerResponse stubResponse = Mockito.mock(
                    InspectContainerResponse.class,
                    Answers.RETURNS_DEEP_STUBS
                );
                Mockito
                    .when(stubResponse.getNetworkSettings().getPorts().getBindings())
                    .thenReturn(Collections.emptyMap());
                return stubResponse;
            };
            return new InspectContainerCmdImpl(exec, invocation.getArgument(0));
        };
    }
}
