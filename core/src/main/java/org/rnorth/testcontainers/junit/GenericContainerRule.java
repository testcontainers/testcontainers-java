package org.rnorth.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.rnorth.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericContainerRule allows for any public docker image to be used for a container.
 *
 * Custom settings may be configured via a fluent interface.
 */
public class GenericContainerRule extends ExternalResource {

    private final GenericContainer container;

    public GenericContainerRule(String dockerImageName) {
        this.container = new GenericContainer(dockerImageName);
    }

    public GenericContainerRule withExposedPorts(int... ports) {
        String[] stringValues = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            stringValues[i] = String.valueOf(ports[i]);
        }

        return withExposedPorts(stringValues);
    }

    public GenericContainerRule withExposedPorts(String... ports) {
        List<String> portsWithSuffix = new ArrayList<>();

        for (String rawPort : ports) {
            if (rawPort.contains("/")) {
                portsWithSuffix.add(rawPort);
            } else {
                portsWithSuffix.add(rawPort + "/tcp");
            }
        }

        container.setExposedPorts(portsWithSuffix);
        return this;
    }

    public GenericContainerRule withEnv(String key, String value) {
        container.addEnv(key, value);
        return this;
    }

    public GenericContainerRule withCommand(String cmd) {
        container.setCommand(cmd);
        return this;
    }

    public GenericContainerRule withCommand(String... commandParts) {
        container.setCommand(commandParts);
        return this;
    }

    public String getIpAddress() {
        return container.getIpAddress();
    }

    public String getPort(int port) {
        return container.getPort(port + "/tcp");
    }

    public String getPort(String port) {
        if (port.contains("/")) {
            return container.getPort(port);
        } else {
            return container.getPort(port + "/tcp");
        }
    }

    @Override
    protected void before() throws Throwable {
        container.start();
    }

    @Override
    protected void after() {
        container.stop();
    }
}
