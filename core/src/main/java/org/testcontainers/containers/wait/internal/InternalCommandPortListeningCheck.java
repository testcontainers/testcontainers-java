package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.util.Set;

import static java.lang.String.format;

/**
 * Mechanism for testing that a socket is listening when run from the container being checked.
 */
@RequiredArgsConstructor
public class InternalCommandPortListeningCheck implements java.util.concurrent.Callable<Boolean> {

    private static final String SUCCESS_MARKER = "TESTCONTAINERS_SUCCESS";

    private final WaitStrategyTarget waitStrategyTarget;
    private final Set<Integer> internalPorts;

    @Override
    public Boolean call() {
        for (Integer internalPort : internalPorts) {
            tryPort(internalPort);
        }

        return true;
    }

    private void tryPort(Integer internalPort) {
        String[][] commands = {
                {"/bin/sh", "-c", format("cat /proc/net/tcp{,6} | awk '{print $2}' | grep -i :%x && echo %s", internalPort, SUCCESS_MARKER)},
                {"/bin/sh", "-c", format("nc -vz -w 1 localhost %d && echo %s", internalPort, SUCCESS_MARKER)},
                {"/bin/bash", "-c", format("</dev/tcp/localhost/%d && echo %s", internalPort, SUCCESS_MARKER)}
        };

        for (String[] command : commands) {
            try {
                if (ExecInContainerPattern.execInContainer(waitStrategyTarget.getContainerInfo(), command).getStdout().contains(SUCCESS_MARKER)) {
                    return;
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        throw new IllegalStateException("Socket not listening yet: " + internalPort);
    }
}
