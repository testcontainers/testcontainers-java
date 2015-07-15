package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.rnorth.testcontainers.containers.traits.LinkableContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rnorth on 14/07/2015.
 */
public class GenericContainer extends AbstractContainer implements LinkableContainer {

    private final String dockerImageName;
    private List<String> exposedPorts;

    private Map<String, List<PortBinding>> ports;

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
            return getPort(exposedPorts.get(0));
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
     * @param exposedPorts
     */
    public void setExposedPorts(List<String> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    /**
     * Set the command that should be run in the container
     * @param command
     */
    public void setCommand(String command) {
        this.commandParts = command.split(" ");
    }

    /**
     * Set the command that should be run in the container
     * @param commandParts
     */
    public void setCommand(String[] commandParts) {
        this.commandParts = commandParts;
    }

    public String getIpAddress() {
        return dockerHostIpAddress;
    }

    public String getPort(String port) {
        if (ports != null) {

            List<PortBinding> usingSuffix = ports.get(port + "/tcp");
            List<PortBinding> withoutSuffix = ports.get(port);

            return usingSuffix != null && usingSuffix.get(0) != null ? usingSuffix.get(0).hostPort() : withoutSuffix.get(0).hostPort();
        } else {
            return null;
        }
    }

    public void addEnv(String key, String value) {
        env.add(key + "=" + value);
    }
}
