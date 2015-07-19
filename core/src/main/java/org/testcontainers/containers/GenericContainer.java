package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.testcontainers.containers.traits.LinkableContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GenericContainer allows any docker image to be used to create a container.
 *
 * No specific helpers are provided for using container; instead, raw port, environment and command configuration
 * may be used.
 */
public class GenericContainer extends AbstractContainer implements LinkableContainer {

    private final String dockerImageName;
    private List<String> exposedPorts;

    protected Map<String, List<PortBinding>> ports;

    private List<String> env = new ArrayList<>();
    private String[] commandParts;

    public GenericContainer(final String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        ports = containerInfo.networkSettings().ports();
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        ContainerConfig.Builder builder = ContainerConfig.builder()
                .image(getDockerImageName());

        if (exposedPorts != null) {
            builder = builder.exposedPorts(exposedPorts.toArray(new String[exposedPorts.size()]));
        }

        if (commandParts != null) {
            builder = builder.cmd(commandParts);
        }

        if (!env.isEmpty()) {
            builder = builder.env(env);
        }

        return builder
                .build();
    }

    @Override
    protected String getLivenessCheckPort() {
        if (exposedPorts.size() > 0) {
            return getMappedPort(exposedPorts.get(0));
        } else {
            return null;
        }
    }

    @Override
    protected String getDockerImageName() {
        return dockerImageName;
    }

    /**
     * Set the ports that this container listens on
     * @param exposedPorts a list of ports in 'number/protocol' format, e.g. '80/tcp'
     */
    public void setExposedPorts(List<String> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    /**
     * Set the command that should be run in the container
     * @param command a command in single string format (will automatically be split on spaces)
     */
    public void setCommand(String command) {
        this.commandParts = command.split(" ");
    }

    /**
     * Set the command that should be run in the container
     * @param commandParts a command as an array of string parts
     */
    public void setCommand(String[] commandParts) {
        this.commandParts = commandParts;
    }

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     * @return an IP address
     */
    public String getIpAddress() {
        return dockerHostIpAddress;
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort should be a String either containing just the port number or suffixed '/tcp', e.g. '80/tcp'
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    public String getMappedPort(String originalPort) {
        if (ports != null) {

            List<PortBinding> usingSuffix = ports.get(originalPort + "/tcp");
            List<PortBinding> withoutSuffix = ports.get(originalPort);

            if (usingSuffix != null && usingSuffix.get(0) != null) {
                return usingSuffix.get(0).hostPort();
            } else {
                return withoutSuffix.get(0).hostPort();
            }
        } else {
            return null;
        }
    }

    /**
     * Add an environment variable to be passed to the container.
     * @param key environment variable key
     * @param value environment variable value
     */
    public void addEnv(String key, String value) {
        env.add(key + "=" + value);
    }
}
