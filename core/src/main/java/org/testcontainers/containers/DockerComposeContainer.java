package org.testcontainers.containers;

import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Filters;
import com.google.common.util.concurrent.Uninterruptibles;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.profiler.Profiler;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.ContainerReaper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * Container which launches Docker Compose, for the purposes of launching a defined set of containers.
 */
public class DockerComposeContainer<SELF extends DockerComposeContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {

    /**
     * Random identifier which will become part of spawned containers names, so we can shut them down
     */
    private final String identifier;
    private final Map<String, AmbassadorContainer> ambassadorContainers = new HashMap<>();
    private Set<String> spawnedContainerIds;

    public DockerComposeContainer(File composeFile) {
        this(composeFile, "up -d");
    }

    @SuppressWarnings("WeakerAccess")
    public DockerComposeContainer(File composeFile, String command) {
        super("dduportal/docker-compose:1.6.0");

        // Create a unique identifier and tell compose
        identifier = Base58.randomString(6).toLowerCase();
        addEnv("COMPOSE_PROJECT_NAME", identifier);

        // Map the docker compose file into the container
        addEnv("COMPOSE_FILE", "/compose/" + composeFile.getAbsoluteFile().getName());
        addFileSystemBind(composeFile.getAbsoluteFile().getParentFile().getAbsolutePath(), "/compose", READ_ONLY);

        // Ensure that compose can access docker. Since the container is assumed to be running on the same machine
        //  as the docker daemon, just mapping the docker control socket is OK.
        // As there seems to be a problem with mapping to the /var/run directory in certain environments (e.g. CircleCI)
        //  we map the socket file outside of /var/run, as just /docker.sock
        addFileSystemBind("/var/run/docker.sock", "/docker.sock", READ_WRITE);
        addEnv("DOCKER_HOST", "unix:///docker.sock");

        if (command != null) {
            setCommand(command);
        }
    }

    @Override
    public void start() {

        final Profiler profiler = new Profiler("Docker compose container rule");
        profiler.setLogger(logger());
        profiler.start("Docker compose container startup");

        // Start the docker-compose container, which starts up the services
        super.start();
        followOutput(new Slf4jLogConsumer(logger()), OutputFrame.OutputType.STDERR);

        // wait for the compose container to stop, which should only happen after it has spawned all the service containers
        logger().info("Docker compose container is running - service creation will start now");
        while (isRunning()) {
            logger().trace("Compose container is still running");
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        logger().info("Docker compose has finished running");

        // Ensure that all service containers that were launched by compose will be killed at shutdown
        Filters namesContainingComposeIdentifier = new Filters().withFilter("name", "/" + identifier);
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withFilters(namesContainingComposeIdentifier)
                    .withShowAll(true)
                    .exec();

            // register with ContainerReaper to ensure final shutdown with JVM
            containers.stream()
                    .forEach(container ->
                            ContainerReaper.instance().registerContainerForCleanup(container.getId(), container.getNames()[0]));

            // remember the IDs to allow containers to be killed as soon as we reach stop()
            spawnedContainerIds = containers.stream()
                    .map(Container::getId)
                    .collect(Collectors.toSet());

        } catch (DockerException e) {
            logger().debug("Failed to stop a service container with exception", e);
        }

        for (final Map.Entry<String, AmbassadorContainer> address : ambassadorContainers.entrySet()) {

            try {
                // Start any ambassador containers we need
                profiler.start("Ambassador container startup");

                final AmbassadorContainer ambassadorContainer = address.getValue();
                Unreliables.retryUntilSuccess(120, TimeUnit.SECONDS, () -> {
                    Profiler localProfiler = profiler.startNested("Ambassador container: " + ambassadorContainer.getContainerName());

                    localProfiler.start("Start ambassador container");

                    ambassadorContainer.start();

                    return null;
                });
            } catch (Exception e) {
                logger().warn("Exception during ambassador container startup!", e);
            } finally {
                profiler.stop().log();
            }
        }
    }

    @Override
    public void stop() {
        // this, the compose container, should not be running, but just in case something has gone wrong
        super.stop();

        // shut down all the ambassador containers
        ambassadorContainers.forEach((String address, AmbassadorContainer container) -> container.stop());

        // kill the spawned service containers
        spawnedContainerIds.forEach(id -> ContainerReaper.instance().stopAndRemoveContainer(id));
        spawnedContainerIds.clear();
    }

    @Override
    @Deprecated
    public SELF withExposedPorts(Integer... ports) {
        throw new UnsupportedOperationException("Use withExposedService instead");
    }

    public SELF withExposedService(String serviceName, int servicePort) {

        /**
         * For every service/port pair that needs to be exposed, we have to start an 'ambassador container'.
         *
         * The ambassador container's role is to link (within the Docker network) to one of the
         * compose services, and proxy TCP network I/O out to a port that the ambassador container
         * exposes.
         *
         * This avoids the need for the docker compose file to explicitly expose ports on all the
         * services.
         */
        AmbassadorContainer ambassadorContainer = new AmbassadorContainer(new FutureContainer(this.identifier + "_" + serviceName), serviceName, servicePort);

        // Ambassador containers will all be started together after docker compose has started
        ambassadorContainers.put(serviceName + ":" + servicePort, ambassadorContainer);

        return self();
    }

    /**
     * Get the host (e.g. IP address or hostname) that an exposed service can be found at, from the host machine
     * (i.e. should be the machine that's running this Java process).
     * <p>
     * The service must have been declared using DockerComposeContainer#withExposedService.
     *
     * @param serviceName the name of the service as set in the docker-compose.yml file.
     * @param servicePort the port exposed by the service container.
     * @return a host IP address or hostname that can be used for accessing the service container.
     */
    public String getServiceHost(String serviceName, Integer servicePort) {
        return ambassadorContainers.get(serviceName + ":" + servicePort).getContainerIpAddress();
    }

    /**
     * Get the port that an exposed service can be found at, from the host machine
     * (i.e. should be the machine that's running this Java process).
     * <p>
     * The service must have been declared using DockerComposeContainer#withExposedService.
     *
     * @param serviceName the name of the service as set in the docker-compose.yml file.
     * @param servicePort the port exposed by the service container.
     * @return a port that can be used for accessing the service container.
     */
    public Integer getServicePort(String serviceName, Integer servicePort) {
        return ambassadorContainers.get(serviceName + ":" + servicePort).getMappedPort(servicePort);
    }
}
