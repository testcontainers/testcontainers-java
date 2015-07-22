package org.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * GenericContainerRule allows for any public docker image to be used for a container.
 *
 * Custom settings may be configured via a fluent interface.
 */
public class GenericContainerRule extends ExternalResource {

    protected final GenericContainer container;

    public GenericContainerRule(String dockerImageName) {
        this.container = new GenericContainer(dockerImageName);
    }

    public GenericContainerRule(GenericContainer container) {
        this.container = container;
    }

    /**
     * Set the ports that this container listens on
     * @param ports an array of TCP ports
     * @return this
     */
    public GenericContainerRule withExposedPorts(int... ports) {
        String[] stringValues = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            stringValues[i] = String.valueOf(ports[i]);
        }

        return withExposedPorts(stringValues);
    }

    /**
     * Set the ports that this container listens on
     * @param ports an array of ports in either 'port/protocol' format (e.g. '80/tcp') or 'port' format (e.g. '80')
     * @return this
     */
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

    /**
     * Add an environment variable to be passed to the container.
     * @param key environment variable key
     * @param value environment variable value
     * @return this
     */
    public GenericContainerRule withEnv(String key, String value) {
        container.addEnv(key, value);
        return this;
    }

    /**
     * Set the command that should be run in the container
     * @param cmd a command in single string format (will automatically be split on spaces)
     * @return this
     */
    public GenericContainerRule withCommand(String cmd) {
        container.setCommand(cmd);
        return this;
    }

    /**
     * Set the command that should be run in the container
     * @param commandParts a command as an array of string parts
     * @return this
     */
    public GenericContainerRule withCommand(String... commandParts) {
        container.setCommand(commandParts);
        return this;
    }

    /**
     * Map a resource (file or directory) on the classpath to a path inside the container
     * @param resourcePath path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
     * @param containerPath path this should be mapped to inside the container
     * @param mode access mode for the file
     * @return this
     */
    public GenericContainerRule withClasspathResourceMapping(String resourcePath, String containerPath, GenericContainer.BindMode mode) {
        URL resource = GenericContainerRule.class.getClassLoader().getResource(resourcePath);

        if (resource == null) {
            throw new IllegalArgumentException("Could not find classpath resource at provided path: " + resourcePath);
        }
        String resourceFilePath = resource.getFile();

        container.addFileSystemBind(resourceFilePath, containerPath, mode);

        return this;
    }

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     * @return an IP address
     */
    public String getIpAddress() {
        return container.getIpAddress();
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    public String getMappedPort(int originalPort) {
        return container.getMappedPort(originalPort + "/tcp");
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort should be a String either containing just the port number or suffixed '/tcp', e.g. '80/tcp'
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    public String getMappedPort(String originalPort) {
        if (originalPort.contains("/")) {
            return container.getMappedPort(originalPort);
        } else {
            return container.getMappedPort(originalPort + "/tcp");
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
