package org.testcontainers;

import java.util.Map;
import java.util.Map.Entry;

import org.testcontainers.containers.PortForwardingContainer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Testcontainers {

    public void exposeHostPorts(int... ports) {
        for (int port : ports) {
            PortForwardingContainer.INSTANCE.exposeHostPort(port);
        }
    }
    
    public void exposeHostPorts(Map<Integer, Integer> ports) {
    	for(Entry<Integer, Integer> entry : ports.entrySet()) {
    		PortForwardingContainer.INSTANCE.exposeHostPort(entry.getKey(), entry.getValue());
    	}
    }
}
