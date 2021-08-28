package org.testcontainers.utility;

import org.testcontainers.ContainerControllerFactory;
import org.testcontainers.controller.ContainerProvider;
import org.testcontainers.docker.DockerClientFactory;
import org.testcontainers.dockerclient.DockerMachineClientProviderStrategy;

/**
 * Provides utility methods for determining facts about the test environment.
 */
public class TestEnvironment {

    private TestEnvironment() {
    }

    public static boolean dockerApiAtLeast(String minimumVersion) {
        ComparableVersion min = new ComparableVersion(minimumVersion);
        ComparableVersion current = new ComparableVersion(DockerClientFactory.instance().getActiveApiVersion());

        return current.compareTo(min) >= 0;
    }

    /**
     *
     * @deprecated Use {@link ContainerProvider#supportsExecution()}
     */
    @Deprecated // TODO: Remove
    public static boolean dockerExecutionDriverSupportsExec() {
        return ContainerControllerFactory.instance().supportsExecution();
    }

    public static boolean dockerIsDockerMachine() {
        return DockerClientFactory.instance().isUsing(DockerMachineClientProviderStrategy.class);
    }
}

