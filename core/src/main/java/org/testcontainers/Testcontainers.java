package org.testcontainers;

import lombok.experimental.UtilityClass;
import org.testcontainers.containers.PortForwardingContainer;

@UtilityClass
public class Testcontainers {

    public void exposeHostPorts(int... ports) {
        for (int port : ports) {
            PortForwardingContainer.INSTANCE.exposeHostPort(port);
        }
    }
}
