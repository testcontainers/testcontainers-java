package org.testcontainers.containers;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import lombok.EqualsAndHashCode;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Class to provide access to containers started through docker-compose
 */
@SuppressWarnings("ConstantConditions")
@EqualsAndHashCode(callSuper = true)
public class DockerComposeServiceInstance extends AbstractContainerState {

    private final Container container;
    private final GenericContainer proxyContainer;
    private Map<Integer, Integer> mappedPorts = new HashMap<>();

    DockerComposeServiceInstance(Container container, GenericContainer proxyContainer,
                                 Map<Integer, Integer> mappedPorts) {
        this.container = container;
        this.containerId = this.container.getId();
        this.containerInfo = dockerClient.inspectContainerCmd(this.containerId).exec();
        this.containerName = this.containerInfo.getName();

        this.proxyContainer = proxyContainer;
        if (mappedPorts != null) {
            this.mappedPorts.putAll(mappedPorts);
            this.exposedPorts .addAll(this.mappedPorts.keySet());
        }

        this.setImage();
        this.setPortBindings();
        this.setEnv();
        this.setCommandParts();
        this.setBinds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMappedPort(int originalPort) {
        return proxyContainer.getMappedPort(mappedPorts.get(originalPort));
    }

    private void setPortBindings() {
        List<String> portBindings = new ArrayList<>();
        final Ports hostPortBindings = this.containerInfo.getHostConfig().getPortBindings();

        for (Map.Entry<ExposedPort, Ports.Binding[]> binding : hostPortBindings.getBindings().entrySet()) {
            for (Ports.Binding portBinding : binding.getValue()) {
                portBindings.add(String.format("%s:%s", portBinding.toString(), binding.getKey()));
            }
        }
        this.portBindings = portBindings;
    }

    @Override
    public List<String> getExtraHosts() {
        final String[] extraHosts = this.containerInfo.getHostConfig().getExtraHosts();
        if (extraHosts != null) {
            return Arrays.asList(extraHosts);
        } else {
            return Collections.emptyList();
        }
    }

    private void setImage() {
        String imageName = this.container.getImage();
        try {
            DockerImageName.validate(imageName);
        } catch (IllegalArgumentException e) {
            //if the image name does not have a label, used the latest label
            imageName = String.format("%s:latest", imageName);
        }

        this.image = new RemoteDockerImage(imageName);
    }

    private void setEnv() {
        final String[] containerEnv = this.containerInfo.getConfig().getEnv();

        if (env != null) {
            this.env = Arrays.stream(containerEnv)
                .map(envVar -> envVar.split("="))
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
        }
    }

    private void setCommandParts() {
        this.commandParts = this.container.getCommand().split(" ");
    }

    private void setBinds() {
        this.binds = this.containerInfo.getMounts()
            .stream()
            .map(mount -> new Bind(MountableFile.forHostPath(mount.getSource()).getResolvedPath(),
                new Volume(mount.getDestination().getPath())))
            .collect(Collectors.toList());
    }
}
