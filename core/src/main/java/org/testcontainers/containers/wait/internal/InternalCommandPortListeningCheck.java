package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static java.lang.String.format;

/**
 * Mechanism for testing that a socket is listening when run from the container being checked.
 */
@RequiredArgsConstructor
@Slf4j
public class InternalCommandPortListeningCheck implements java.util.concurrent.Callable<Boolean> {

    private final WaitStrategyTarget waitStrategyTarget;
    private final Set<Integer> internalPorts;

    @Override
    public Boolean call() {
        StringBuilder command = new StringBuilder("true");

        for (int internalPort : internalPorts) {
            command.append(" && ");
            command.append(" (");
            command.append(format("cat /proc/net/tcp* | awk '{print $2}' | grep -i ':0*%x'", internalPort));
            command.append(" || ");
            command.append(format("nc -vz -w 1 localhost %d", internalPort));
            command.append(" || ");
            command.append(format("/bin/bash -c '</dev/tcp/localhost/%d'", internalPort));
            command.append(")");
        }

        Instant before = Instant.now();
        try {
            ExecResult result = ExecInContainerPattern.execInContainer(waitStrategyTarget.getContainerInfo(), "/bin/sh", "-c", command.toString());
            log.trace("Check for {} took {}. Result code '{}', stdout message: '{}'", internalPorts, Duration.between(before, Instant.now()), result.getExitCode(), result.getStdout());
            int exitCode = result.getExitCode();
            if (exitCode != 0 && exitCode != 1) {
                log.warn("An exception while executing the internal check: {}", result);
            }
            return exitCode == 0;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
