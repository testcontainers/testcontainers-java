package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testcontainers.TestImages;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

/**
 * Covers the cross-process arbiter in {@code GenericContainer#createOrJoinReusableContainer}:
 * when the JVM-local lock can't help (a different process/JVM raced us), the deterministic
 * container name derived from the reuse hash makes Docker's own name-uniqueness constraint the
 * tiebreaker. These tests simulate the name conflict that a concurrent process's "docker create"
 * would produce, without needing an actual second process.
 *
 * <p>Since the real reuse hash depends on the fully-configured {@code CreateContainerCmd} (labels,
 * host config, etc. are only set by {@code applyConfiguration} right before hashing), these tests
 * capture the hash {@code GenericContainer} actually computes, via {@link GenericContainer#HASH_LABEL}
 * on the command passed to the mocked "create" call, rather than recomputing it independently,
 * which would silently drift from the real value and invalidate the label-matching assertions.
 */
class ReusabilityNameConflictTest extends ReusabilityUnitTests.AbstractReusabilityTest {

    @Test
    void reusesConflictingContainerWhenLabelMatchesAndRunning() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        String winnerContainerId = "winner-container-id";
        AtomicReference<String> capturedHash = new AtomicReference<>();
        AtomicInteger createAttempts = new AtomicInteger();

        when(client.createContainerCmd(any())).then(conflictOnceThenCapture(createAttempts, capturedHash));
        when(client.listContainersCmd()).then(listContainersAnswer());
        when(client.inspectContainerCmd(startsWith(GenericContainer.REUSE_CONTAINER_NAME_PREFIX)))
            .then(inspectAnswer(() -> inspectResponse(winnerContainerId, "running", labelsFor(capturedHash.get()))));
        // Once reused, tryStart() inspects by container id (not by the reuse name) to wait for
        // mapped ports, so this needs its own stub distinct from the name-based one above.
        when(client.inspectContainerCmd(winnerContainerId)).then(inv -> inspectContainerAnswer().answer(inv));

        GenericContainer<?> container = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));
        container.start();

        assertThat(container.getContainerId()).isEqualTo(winnerContainerId);
        assertThat(createAttempts.get())
            .as("create attempts before finding the running winner")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    void refusesToReuseConflictingContainerWithMismatchedLabel() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        when(client.createContainerCmd(any()))
            .then(invocation -> {
                CreateContainerCmd.Exec exec = command -> {
                    throw new ConflictException("container name already in use");
                };
                return new CreateContainerCmdImpl(exec, null, "image:latest");
            });
        when(client.listContainersCmd()).then(listContainersAnswer());
        when(client.inspectContainerCmd(startsWith(GenericContainer.REUSE_CONTAINER_NAME_PREFIX)))
            .then(
                inspectAnswer(() -> {
                    return inspectResponse("unrelated-container-id", "running", labelsFor("some-other-hash-entirely"));
                })
            );

        GenericContainer<?> container = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));

        // doStart() retries tryStart() via Unreliables.retryUntilSuccess, so the IllegalStateException
        // thrown by createOrJoinReusableContainer ends up as the *root* cause, wrapped first in a
        // ContainerLaunchException by tryStart()'s catch block and then again after the retry gives up.
        assertThatThrownBy(container::start)
            .hasRootCauseInstanceOf(IllegalStateException.class)
            .hasStackTraceContaining("is already in use by a container that is not a Testcontainers reuse container");
    }

    @Test
    void retriesWhileConflictingContainerIsStillBeingCreated() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        String winnerContainerId = "winner-container-id";
        AtomicReference<String> capturedHash = new AtomicReference<>();
        AtomicInteger createAttempts = new AtomicInteger();
        AtomicInteger inspectCalls = new AtomicInteger();

        when(client.createContainerCmd(any())).then(conflictOnceThenCapture(createAttempts, capturedHash));
        when(client.listContainersCmd()).then(listContainersAnswer());

        // First two inspects see the winner still in "created" state (not started yet), the third
        // sees it "running". The retry/backoff loop must ride this out rather than giving up or
        // misreporting a name clash.
        when(client.inspectContainerCmd(startsWith(GenericContainer.REUSE_CONTAINER_NAME_PREFIX)))
            .then(
                inspectAnswer(() -> {
                    int call = inspectCalls.incrementAndGet();
                    String status = call < 3 ? "created" : "running";
                    return inspectResponse(winnerContainerId, status, labelsFor(capturedHash.get()));
                })
            );
        when(client.inspectContainerCmd(winnerContainerId)).then(inv -> inspectContainerAnswer().answer(inv));

        GenericContainer<?> container = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));
        container.start();

        assertThat(container.getContainerId()).isEqualTo(winnerContainerId);
        assertThat(inspectCalls.get()).as("number of re-inspects while waiting").isGreaterThanOrEqualTo(3);
    }

    @Test
    void cleansUpDeadConflictingContainerThenRetriesCreate() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        String deadContainerId = "dead-container-id";
        String freshContainerId = "fresh-container-id";
        AtomicReference<String> capturedHash = new AtomicReference<>();
        AtomicInteger createAttempts = new AtomicInteger();
        AtomicInteger removeAttempts = new AtomicInteger();

        when(client.createContainerCmd(any()))
            .then(invocation -> {
                CreateContainerCmd.Exec exec = command -> {
                    if (capturedHash.get() == null) {
                        capturedHash.set(command.getLabels().get(GenericContainer.HASH_LABEL));
                    }
                    if (createAttempts.incrementAndGet() == 1) {
                        throw new ConflictException("container name already in use");
                    }
                    CreateContainerResponse response = new CreateContainerResponse();
                    response.setId(freshContainerId);
                    return response;
                };
                return new CreateContainerCmdImpl(exec, null, "image:latest");
            });
        when(client.listContainersCmd()).then(listContainersAnswer());
        when(client.startContainerCmd(freshContainerId)).then(inv -> startContainerAnswer().answer(inv));
        when(client.inspectContainerCmd(freshContainerId)).then(inv -> inspectContainerAnswer().answer(inv));
        when(client.inspectContainerCmd(startsWith(GenericContainer.REUSE_CONTAINER_NAME_PREFIX)))
            .then(inspectAnswer(() -> inspectResponse(deadContainerId, "exited", labelsFor(capturedHash.get()))));

        RemoveContainerCmd removeCmd = Mockito.mock(RemoveContainerCmd.class, Answers.RETURNS_SELF);
        when(removeCmd.exec())
            .then(invocation -> {
                removeAttempts.incrementAndGet();
                return null;
            });
        when(client.removeContainerCmd(deadContainerId)).thenReturn(removeCmd);

        GenericContainer<?> container = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));
        container.start();

        assertThat(container.getContainerId()).isEqualTo(freshContainerId);
        assertThat(removeAttempts.get()).as("dead conflicting container removed").isEqualTo(1);
        assertThat(createAttempts.get()).as("create retried after cleanup").isEqualTo(2);
    }

    @Test
    void toleratesConflictingContainerAlreadyRemovedByAConcurrentCleanup() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();

        String freshContainerId = "fresh-container-id";
        AtomicInteger createAttempts = new AtomicInteger();

        when(client.createContainerCmd(any()))
            .then(invocation -> {
                CreateContainerCmd.Exec exec = command -> {
                    if (createAttempts.incrementAndGet() == 1) {
                        throw new ConflictException("container name already in use");
                    }
                    CreateContainerResponse response = new CreateContainerResponse();
                    response.setId(freshContainerId);
                    return response;
                };
                return new CreateContainerCmdImpl(exec, null, "image:latest");
            });
        when(client.listContainersCmd()).then(listContainersAnswer());
        when(client.startContainerCmd(freshContainerId)).then(inv -> startContainerAnswer().answer(inv));
        when(client.inspectContainerCmd(freshContainerId)).then(inv -> inspectContainerAnswer().answer(inv));

        // Simulate a concurrent loser (or Ryuk) already removing the conflicting container by the
        // time we inspect it: NotFoundException, not a running/created/exited container. The
        // arbiter must treat this as "nothing to join, just retry create", not as an error.
        when(client.inspectContainerCmd(startsWith(GenericContainer.REUSE_CONTAINER_NAME_PREFIX)))
            .then(invocation -> {
                InspectContainerCmd cmd = Mockito.mock(InspectContainerCmd.class);
                when(cmd.exec()).thenThrow(new NotFoundException("no such container"));
                return cmd;
            });

        GenericContainer<?> container = makeReusable(new GenericContainer<>(TestImages.TINY_IMAGE));
        container.start();

        assertThat(container.getContainerId()).isEqualTo(freshContainerId);
        assertThat(createAttempts.get()).as("create retried after conflicting container vanished").isEqualTo(2);
    }

    private static org.mockito.stubbing.Answer<CreateContainerCmd> conflictOnceThenCapture(
        AtomicInteger createAttempts,
        AtomicReference<String> capturedHash
    ) {
        return invocation -> {
            CreateContainerCmd.Exec exec = command -> {
                capturedHash.compareAndSet(null, command.getLabels().get(GenericContainer.HASH_LABEL));
                createAttempts.incrementAndGet();
                throw new ConflictException("container name already in use");
            };
            return new CreateContainerCmdImpl(exec, null, "image:latest");
        };
    }

    private static Map<String, String> labelsFor(String hash) {
        return Collections.singletonMap(GenericContainer.HASH_LABEL, hash);
    }

    private static InspectContainerResponse inspectResponse(String id, String status, Map<String, String> labels) {
        InspectContainerResponse response = Mockito.mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(response.getId()).thenReturn(id);
        when(response.getConfig().getLabels()).thenReturn(labels);
        when(response.getState().getStatus()).thenReturn(status);
        return response;
    }

    private static org.mockito.stubbing.Answer<InspectContainerCmd> inspectAnswer(
        Supplier<InspectContainerResponse> resultSupplier
    ) {
        return invocation -> {
            // Resolve the result before opening the when(...) stub: resultSupplier.get() creates
            // and stubs its own mock (see inspectResponse()), and nesting that inside an
            // in-progress when(cmd.exec()) call confuses Mockito's stubbing state.
            InspectContainerResponse result = resultSupplier.get();
            InspectContainerCmd cmd = Mockito.mock(InspectContainerCmd.class);
            when(cmd.exec()).thenReturn(result);
            return cmd;
        };
    }
}
