package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.InternetProtocol;
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

    private final InternetProtocol internetProtocol;

    public InternalCommandPortListeningCheck(WaitStrategyTarget waitStrategyTarget, Set<Integer> internalPorts) {
        this.waitStrategyTarget = waitStrategyTarget;
        this.internalPorts = internalPorts;
        this.internetProtocol = InternetProtocol.TCP;
    }

    @Override
    public Boolean call() {
        String command = "true";

        for (int internalPort : internalPorts) {
            command += " && ";
            command += " (";
            command += format("cat /proc/net/%s* | awk '{print $2}' | grep -i ':0*%x'", internetProtocol.toDockerNotation(), internalPort);
            command += " || ";
            command += format("nc -vz -w 1 localhost %d", internalPort);
            command += " || ";
            command += format("/bin/bash -c '</dev/%s/localhost/%d'", internetProtocol.toDockerNotation(), internalPort);
            command += ")";
        }

        Instant before = Instant.now();
        try {
            ExecResult result = ExecInContainerPattern.execInContainer(waitStrategyTarget.getContainerInfo(), "/bin/sh", "-c", command);
            log.trace("Check for {} took {}", internalPorts, Duration.between(before, Instant.now()));
            return result.getExitCode() == 0;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
