package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class ReusableContainerConcurrencyTest {

    private final DockerClient dockerClient = DockerClientFactory.instance().client();

    // Makes the hash unique per test run, so containers from earlier runs are not reused
    private final String runId = UUID.randomUUID().toString();

    @BeforeEach
    void enableReuse() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
    }

    @AfterEach
    void removeContainers() {
        dockerClient
            .listContainersCmd()
            .withShowAll(true)
            .withLabelFilter(Collections.singletonMap("test-run-id", runId))
            .exec()
            .forEach(it -> dockerClient.removeContainerCmd(it.getId()).withForce(true).exec());
    }

    @Test
    void concurrentStartsShareOneContainer() throws Exception {
        int starts = 8;
        ExecutorService executor = Executors.newFixedThreadPool(starts);
        CountDownLatch go = new CountDownLatch(1);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < starts; i++) {
                futures.add(
                    CompletableFuture.supplyAsync(
                        () -> {
                            GenericContainer<?> container = reusableContainer();
                            try {
                                go.await();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            container.start();
                            return container.getContainerId();
                        },
                        executor
                    )
                );
            }
            go.countDown();

            Set<String> containerIds = futures.stream().map(CompletableFuture::join).collect(Collectors.toSet());

            assertThat(containerIds).hasSize(1);
            assertThat(containersOfThisRun()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void replacesStoppedContainer() {
        GenericContainer<?> first = reusableContainer();
        first.start();
        dockerClient.stopContainerCmd(first.getContainerId()).withTimeout(0).exec();

        GenericContainer<?> second = reusableContainer();
        second.start();

        assertThat(second.getContainerId()).isNotEqualTo(first.getContainerId());
        assertThat(second.isRunning()).isTrue();
        assertThat(containersOfThisRun()).hasSize(1);
    }

    private GenericContainer<?> reusableContainer() {
        return new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("sleep", "infinity")
            .withLabel("test-run-id", runId)
            .withReuse(true);
    }

    private List<String> containersOfThisRun() {
        return dockerClient
            .listContainersCmd()
            .withShowAll(true)
            .withLabelFilter(Collections.singletonMap("test-run-id", runId))
            .exec()
            .stream()
            .map(it -> it.getId())
            .collect(Collectors.toList());
    }
}
