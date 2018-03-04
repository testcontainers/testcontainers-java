package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.VolumesFrom;
import lombok.EqualsAndHashCode;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to provide access to containers started through docker-compose
 */
@SuppressWarnings("ConstantConditions")
@EqualsAndHashCode(callSuper = true)
public class DockerComposeContainerInstance<SELF extends GenericContainer<SELF>> extends GenericContainer<SELF> {

    private final Container container;
    private final GenericContainer proxyContainer;
    private Map<Integer, Integer> mappedPorts = new HashMap<>();

    DockerComposeContainerInstance(Container container, GenericContainer proxyContainer,
                                   Map<Integer, Integer> mappedPorts, WaitStrategy waitStrategy) {
        this.container = container;
        this.proxyContainer = proxyContainer;
        if (mappedPorts != null) {
            this.mappedPorts.putAll(mappedPorts);
        }
        if (waitStrategy != null) {
            this.setWaitStrategy(waitStrategy);
        }
        this.setContainerProperties();
    }

    private void setContainerProperties() {
        this.containerId = this.container.getId();
        InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(this.containerId).exec();
        this.setContainerInfo(containerInfo);
        this.containerName = containerInfo.getName();
        this.withExposedPorts(this.mappedPorts.keySet().toArray(new Integer[0]));
        this.withCommand(this.container.getCommand());
        this.withWorkingDirectory(containerInfo.getConfig().getWorkingDir());
        this.withNetworkMode(containerInfo.getHostConfig().getNetworkMode());
        this.setPrivilegedMode(containerInfo.getHostConfig().getPrivileged());
        this.setImage();
        this.setEnv();
        this.setExtraHosts();
        this.setNetworkProperties();
        this.setPortBindings();
        this.setBinds();
        this.setVolumesFrom();
    }

    @Override
    public void start() {
        //container is started by docker compose, nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMappedPort(int originalPort) {
        return proxyContainer.getMappedPort(mappedPorts.get(originalPort));
    }

    private void setImage() {
        String imageName = this.container.getImage();
        try {
            DockerImageName.validate(imageName);
        } catch (IllegalArgumentException e) {
            //if the image name does not have a label, used the latest label
            imageName = String.format("%s:latest", imageName);
        }

        this.setImage(new RemoteDockerImage(imageName));
    }

    private void setEnv() {
        final String[] env = this.getContainerInfo().getConfig().getEnv();
        if (env != null) {
            Arrays.stream(env)
                .map(envVar -> envVar.split("="))
                .forEach(pair -> this.addEnv(pair[0], pair[1]));
        }
    }

    private void setExtraHosts() {
        final String[] extraHosts = this.getContainerInfo().getHostConfig().getExtraHosts();
        if (extraHosts != null) {
            this.setExtraHosts(Arrays.asList(extraHosts));
        }
    }

    private void setNetworkProperties() {
        List<ContainerNetwork> networks = new ArrayList<>(this.getContainerInfo().getNetworkSettings().getNetworks().values());
        final ContainerNetwork containerNetwork = networks.get(0);
        if (containerNetwork.getAliases() != null) {
            this.setNetworkAliases(containerNetwork.getAliases());
        }

        this.setNetwork(new Network.NetworkImpl(false, null, Collections.emptySet(), containerNetwork.getNetworkID()));
    }

    private void setPortBindings() {
        List<String> portBindings = new ArrayList<>();
        final Ports hostPortBindings = this.getContainerInfo().getHostConfig().getPortBindings();

        for (Map.Entry<ExposedPort, Ports.Binding[]> binding : hostPortBindings.getBindings().entrySet()) {
            for (Ports.Binding portBinding : binding.getValue()) {
                portBindings.add(String.format("%s:%s", portBinding.toString(), binding.getKey()));
            }
        }
        this.setPortBindings(portBindings);
    }

    private void setBinds() {
        this.getContainerInfo().getMounts()
            .forEach(mount -> this.withFileSystemBind(mount.getSource(), mount.getDestination().getPath(),
                mount.getRW() ? BindMode.READ_WRITE : BindMode.READ_ONLY));
    }

    private void setVolumesFrom() {
        final VolumesFrom[] volumesFrom = this.getContainerInfo().getHostConfig().getVolumesFrom();

        if (volumesFrom != null) {
            this.setVolumesFroms(Arrays.asList(volumesFrom));
        }
    }
}
