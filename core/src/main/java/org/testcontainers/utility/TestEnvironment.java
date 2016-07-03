package org.testcontainers.utility;

import org.testcontainers.DockerClientFactory;
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

    public static boolean dockerExecutionDriverSupportsExec() {
        String executionDriver = DockerClientFactory.instance().getActiveExecutionDriver();

        return !executionDriver.startsWith("lxc");
    }

    public static boolean dockerIsDockerMachine() {
        return DockerClientFactory.instance().isUsing(DockerMachineClientProviderStrategy.class);
    }
}

