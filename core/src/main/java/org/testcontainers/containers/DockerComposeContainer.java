package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.runner.Description;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.ResourceReaper;

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
public class DockerComposeContainer<SELF extends DockerComposeContainer<SELF>> extends FailureDetectingExternalResource {

    /**
     * Random identifier which will become part of spawned containers names, so we can shut them down
     */
    private final String identifier;
    private final Map<String, AmbassadorContainer> ambassadorContainers = new HashMap<>();
    private final File composeFile;
    private Set<String> spawnedContainerIds;
    private Map<String, Integer> scalingPreferences = new HashMap<>();
    private DockerClient dockerClient;

    /**
     * Properties that should be passed through to all Compose and ambassador containers (not
     * necessarily to containers that are spawned by Compose itself)
     */
    private Map<String, String> env = new HashMap<>();

    private static final RateLimiter AMBASSADOR_CREATION_RATE_LIMITER = RateLimiterBuilder
            .newBuilder()
            .withRate(6, TimeUnit.MINUTES)
            .withConstantThroughput()
            .build();

    public DockerComposeContainer(File composeFile) {
        this(composeFile, Base58.randomString(6).toLowerCase());
    }

    @SuppressWarnings("WeakerAccess")
    public DockerComposeContainer(File composeFile, String identifier) {
        this.composeFile = composeFile;

        // Use a unique identifier so that containers created for this compose environment can be identified
        this.identifier = identifier;

        this.dockerClient = DockerClientFactory.instance().client();
    }

    @Override @VisibleForTesting
    public void starting(Description description) {
        final Profiler profiler = new Profiler("Docker compose container rule");
        profiler.setLogger(logger());
        profiler.start("Docker compose container startup");

        applyScaling(); // scale before up, so that all scaled instances are available first for linking
        createServices();
        registerContainersForShutdown();
        startAmbassadorContainers(profiler);

    }


    private void createServices() {
        // Start the docker-compose container, which starts up the services
        getDockerCompose("up -d")
                .start();
    }

    private DockerCompose getDockerCompose(String cmd) {
        return new DockerCompose(composeFile, identifier)
                .withCommand(cmd)
                .withEnv(env);
    }

    private void applyScaling() {
        // Apply scaling
        if (!scalingPreferences.isEmpty()) {
            StringBuilder sb = new StringBuilder("scale");
            for (Map.Entry<String, Integer> scale : scalingPreferences.entrySet()) {
                sb.append(" ").append(scale.getKey()).append("=").append(scale.getValue());
            }

            getDockerCompose(sb.toString())
                    .start();
        }
    }

    private void registerContainersForShutdown() {
        // Ensure that all service containers that were launched by compose will be killed at shutdown
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withLabelFilter("name=/" + identifier)
                    .withShowAll(true)
                    .exec();

            // register with ResourceReaper to ensure final shutdown with JVM
            containers.forEach(container ->
                    ResourceReaper.instance().registerContainerForCleanup(container.getId(), container.getNames()[0]));

            // Ensure that the default network for this compose environment, if any, is also cleaned up
            ResourceReaper.instance().registerNetworkForCleanup(identifier + "_default");

            // remember the IDs to allow containers to be killed as soon as we reach stop()
            spawnedContainerIds = containers.stream()
                    .map(Container::getId)
                    .collect(Collectors.toSet());

        } catch (DockerException e) {
            logger().debug("Failed to stop a service container with exception", e);
        }
    }

    private void startAmbassadorContainers(Profiler profiler) {
        for (final Map.Entry<String, AmbassadorContainer> address : ambassadorContainers.entrySet()) {

            try {
                // Start any ambassador containers we need
                profiler.start("Ambassador container startup");

                final AmbassadorContainer ambassadorContainer = address.getValue();
                Unreliables.retryUntilSuccess(120, TimeUnit.SECONDS, () -> {

                    AMBASSADOR_CREATION_RATE_LIMITER.doWhenReady(() -> {
                        Profiler localProfiler = profiler.startNested("Ambassador container: " + ambassadorContainer.getContainerName());

                        localProfiler.start("Start ambassador container");

                        ambassadorContainer.start();
                    });

                    return null;
                });
            } catch (Exception e) {
                logger().warn("Exception during ambassador container startup!", e);
            } finally {
                profiler.stop().log();
            }
        }
    }

    private Logger logger() {
        return LoggerFactory.getLogger(DockerComposeContainer.class);
    }

    @Override @VisibleForTesting
    public void finished(Description description) {

        // Kill the services using docker-compose
        getDockerCompose("kill")
                .start();
        getDockerCompose("rm -f -v")
                .start();

        // shut down all the ambassador containers
        ambassadorContainers.forEach((String address, AmbassadorContainer container) -> container.stop());

        // kill the spawned service containers
        spawnedContainerIds.forEach(id -> ResourceReaper.instance().stopAndRemoveContainer(id));
        spawnedContainerIds.clear();
    }

    public SELF withExposedService(String serviceName, int servicePort) {

        if (! serviceName.matches(".*_[0-9]+")) {
            serviceName += "_1"; // implicit first instance of this service
        }

        /*
         * For every service/port pair that needs to be exposed, we have to start an 'ambassador container'.
         *
         * The ambassador container's role is to link (within the Docker network) to one of the
         * compose services, and proxy TCP network I/O out to a port that the ambassador container
         * exposes.
         *
         * This avoids the need for the docker compose file to explicitly expose ports on all the
         * services.
         *
         * {@link GenericContainer} should ensure that the ambassador container is on the same network
         * as the rest of the compose environment.
         */
        AmbassadorContainer ambassadorContainer =
                new AmbassadorContainer<>(new FutureContainer(this.identifier + "_" + serviceName), serviceName, servicePort)
                        .withEnv(env);

        // Ambassador containers will all be started together after docker compose has started
        ambassadorContainers.put(serviceName + ":" + servicePort, ambassadorContainer);

        return self();
    }

    public DockerComposeContainer withExposedService(String serviceName, int instance, int servicePort) {
        return withExposedService(serviceName + "_" + instance, servicePort);
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

    public SELF withScaledService(String serviceBaseName, int numInstances) {
        scalingPreferences.put(serviceBaseName, numInstances);

        return self();
    }

    public SELF withEnv(String key, String value) {
        env.put(key, value);
        return self();
    }

    public SELF withEnv(Map<String, String> env) {
        env.forEach(env::put);
        return self();
    }

    private SELF self() {
        return (SELF) this;
    }
}

class DockerCompose extends GenericContainer<DockerCompose> {
    public DockerCompose(File composeFile, String identifier) {

        super("dduportal/docker-compose:1.6.0");
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
        setStartupCheckStrategy(new OneShotStartupCheckStrategy());
    }

    @Override
    public void start() {
        super.start();

        this.followOutput(new Slf4jLogConsumer(logger()), OutputFrame.OutputType.STDERR);

        // wait for the compose container to stop, which should only happen after it has spawned all the service containers
        logger().info("Docker compose container is running for command: {}", Joiner.on(" ").join(this.getCommandParts()));
        while (this.isRunning()) {
            logger().trace("Compose container is still running");
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        logger().info("Docker compose has finished running");
    }
}