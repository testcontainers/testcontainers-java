package org.testcontainers.containers;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class ServicebusEmulatorContainerTest {
    @Rule
    public ServicebusEmulatorContainer servicebusEmulatorContainer = new ServicebusEmulatorContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-messaging/servicebus-emulator")
    );

    @Test
    public void testWIthASBClient() {
        String containerId = servicebusEmulatorContainer.getContainerId();
    }
}
