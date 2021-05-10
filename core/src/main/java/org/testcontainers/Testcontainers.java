package org.testcontainers;

import java.util.Map;

import lombok.experimental.UtilityClass;

import org.testcontainers.containers.PortForwardingContainer;

@UtilityClass
public class Testcontainers {

    public void exposeHostPorts(int... ports) {
        for (int port : ports) {
            PortForwardingContainer.INSTANCE.exposeHostPort(port);
        }
    }
    
    public void exposeHostPorts(Map<Integer, Integer> ports) {
    	ports.forEach(PortForwardingContainer.INSTANCE::exposeHostPort);
    }
}
