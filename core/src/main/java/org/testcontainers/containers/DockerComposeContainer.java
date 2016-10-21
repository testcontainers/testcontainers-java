package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.lang.SystemUtils;
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
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.*;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
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
    private final List<File> composeFiles;
    private Set<String> spawnedContainerIds;
    private Map<String, Integer> scalingPreferences = new HashMap<>();
    private DockerClient dockerClient;
    private boolean localCompose;
    private boolean pull = true;
    private boolean tailChildContainers;

    private static final Object MUTEX = new Object();

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

    @Deprecated
    public DockerComposeContainer(File composeFile, String identifier) {
        this(identifier, composeFile);
    }

    public DockerComposeContainer(File... composeFiles) {
        this(Arrays.asList(composeFiles));
    }

    public DockerComposeContainer(List<File> composeFiles) {
        this(Base58.randomString(6).toLowerCase(), composeFiles);
    }

    public DockerComposeContainer(String identifier, File... composeFiles) {
        this(identifier, Arrays.asList(composeFiles));
    }

    public DockerComposeContainer(String identifier, List<File> composeFiles) {

        this.composeFiles = composeFiles;

        // Use a unique identifier so that containers created for this compose environment can be identified
        this.identifier = identifier;

        this.dockerClient = DockerClientFactory.instance().client();
    }

    @Override
    @VisibleForTesting
    public void starting(Description description) {
        final Profiler profiler = new Profiler("Docker Compose container rule");
        profiler.setLogger(logger());
        profiler.start("Docker Compose container startup");

        synchronized (MUTEX) {
            if (pull) {
                pullImages();
            }
            applyScaling(); // scale before up, so that all scaled instances are available first for linking
            createServices();
            if (tailChildContainers) {
                tailChildContainerLogs();
            }
            registerContainersForShutdown();
            startAmbassadorContainers(profiler);
        }
    }

    private void pullImages() {
        getDockerCompose("pull")
                .start();
    }


    private void createServices() {
        // Start the docker-compose container, which starts up the services
        getDockerCompose("up -d")
                .start();
    }

    private void tailChildContainerLogs() {
        listChildContainers().forEach(container ->
                LogUtils.followOutput(dockerClient,
                        container.getId(),
                        new Slf4jLogConsumer(logger()).withPrefix(container.getNames()[0]),
                        OutputFrame.OutputType.STDOUT,
                        OutputFrame.OutputType.STDERR)
        );
    }

    private DockerCompose getDockerCompose(String cmd) {
        final DockerCompose dockerCompose;
        if (localCompose) {
            dockerCompose = new LocalDockerCompose(composeFiles, identifier);
        } else {
            dockerCompose = new ContainerisedDockerCompose(composeFiles, identifier);
        }
        return dockerCompose
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
            final List<Container> containers = listChildContainers();

            // register with ResourceReaper to ensure final shutdown with JVM
            containers.forEach(container ->
                    ResourceReaper.instance().registerContainerForCleanup(container.getId(), container.getNames()[0]));

            // Ensure that the default network for this compose environment, if any, is also cleaned up
            ResourceReaper.instance().registerNetworkForCleanup(identifier + "_default");
            // Compose can define their own networks as well; ensure these are cleaned up
            dockerClient.listNetworksCmd().exec().forEach(network -> {
                if (network.getName().contains(identifier)) {
                    ResourceReaper.instance().registerNetworkForCleanup(network.getName());
                }
            });

            // remember the IDs to allow containers to be killed as soon as we reach stop()
            spawnedContainerIds = containers.stream()
                    .map(Container::getId)
                    .collect(Collectors.toSet());

        } catch (DockerException e) {
            logger().debug("Failed to stop a service container with exception", e);
        }
    }

    private List<Container> listChildContainers() {
        return dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec().stream()
                .filter(container -> Arrays.stream(container.getNames()).anyMatch(name ->
                        name.startsWith("/" + identifier)))
                .collect(Collectors.toList());
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


        synchronized (MUTEX) {
            // shut down all the ambassador containers
            ambassadorContainers.forEach((String address, AmbassadorContainer container) -> container.stop());

            // Kill the services using docker-compose
            getDockerCompose("down -v")
                    .start();

            // remove the networks before removing the containers
            ResourceReaper.instance().removeNetworks(identifier);

            // kill the spawned service containers
            spawnedContainerIds.forEach(id -> ResourceReaper.instance().stopAndRemoveContainer(id));
            spawnedContainerIds.clear();
        }
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
        env.forEach(this.env::put);
        return self();
    }

    /**
     * Use a local Docker Compose binary instead of a container.
     *
     * @return this instance, for chaining
     */
    public SELF withLocalCompose(boolean localCompose) {
        this.localCompose = localCompose;
        return self();
    }

    /**
     * Whether to pull images first.
     *
     * @return this instance, for chaining
     */
    public SELF withPull(boolean pull) {
        this.pull = pull;
        return self();
    }

    /**
     * Whether to tail child container logs.
     *
     * @return this instance, for chaining
     */
    public SELF withTailChildContainers(boolean tailChildContainers) {
        this.tailChildContainers = tailChildContainers;
        return self();
    }

    private SELF self() {
        return (SELF) this;
    }
}

interface DockerCompose {
    String ENV_PROJECT_NAME = "COMPOSE_PROJECT_NAME";
    String ENV_COMPOSE_FILE = "COMPOSE_FILE";

    DockerCompose withCommand(String cmd);

    DockerCompose withEnv(Map<String, String> env);

    void start();

    default void validateFileList(List<File> composeFiles) {
        checkNotNull(composeFiles);
        checkArgument(!composeFiles.isEmpty(), "No docker compose file have been provided");
    }
}

/**
 * Use Docker Compose container.
 */
class ContainerisedDockerCompose extends GenericContainer<ContainerisedDockerCompose> implements DockerCompose {
    public ContainerisedDockerCompose(List<File> composeFiles, String identifier) {

        super("docker/compose:1.8.0");
        validateFileList(composeFiles);

        addEnv(ENV_PROJECT_NAME, identifier);

        // Map the docker compose file into the container
        final File dockerComposeBaseFile = composeFiles.get(0);
        final String pwd = dockerComposeBaseFile.getAbsoluteFile().getParentFile().getAbsolutePath();
        final String containerPwd;
        if (SystemUtils.IS_OS_WINDOWS) {
            containerPwd = PathUtils.createMinGWPath(pwd).substring(1);
        } else {
            containerPwd = pwd;
        }

        final List<String> absoluteDockerComposeFiles = composeFiles.stream().map(
                file -> containerPwd + "/" + file.getAbsoluteFile().getName()).collect(Collectors.toList());
        final String composeFileEnvVariableValue = Joiner.on(File.pathSeparator).join(absoluteDockerComposeFiles);
        logger().debug("Set env COMPOSE_FILE={}", composeFileEnvVariableValue);
        addEnv(ENV_COMPOSE_FILE, composeFileEnvVariableValue);
        addFileSystemBind(pwd, containerPwd, READ_ONLY);

        // Ensure that compose can access docker. Since the container is assumed to be running on the same machine
        //  as the docker daemon, just mapping the docker control socket is OK.
        // As there seems to be a problem with mapping to the /var/run directory in certain environments (e.g. CircleCI)
        //  we map the socket file outside of /var/run, as just /docker.sock
        addFileSystemBind("/var/run/docker.sock", "/docker.sock", READ_WRITE);
        addEnv("DOCKER_HOST", "unix:///docker.sock");
        setStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy());
        setWorkingDirectory(containerPwd);
    }

    @Override
    public void start() {
        super.start();

        this.followOutput(new Slf4jLogConsumer(logger()));

        // wait for the compose container to stop, which should only happen after it has spawned all the service containers
        logger().info("Docker Compose container is running for command: {}", Joiner.on(" ").join(this.getCommandParts()));
        while (this.isRunning()) {
            logger().trace("Compose container is still running");
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        logger().info("Docker Compose has finished running");
    }
}

/**
 * Use local Docker Compose binary, if present.
 */
class LocalDockerCompose implements DockerCompose {
    /**
     * Executable name for Docker Compose.
     */
    private static final String COMPOSE_EXECUTABLE = "docker-compose";

    private final List<File> composeFiles;
    private final String identifier;
    private String cmd = "";
    private Map<String, String> env = new HashMap<>();

    public LocalDockerCompose(List<File> composeFiles, String identifier) {
        validateFileList(composeFiles);

        this.composeFiles = composeFiles;
        this.identifier = identifier;
    }

    @Override
    public DockerCompose withCommand(String cmd) {
        this.cmd = cmd;
        return this;
    }

    @Override
    public DockerCompose withEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    @Override
    public void start() {
        // bail out early
        if (!CommandLine.executableExists(COMPOSE_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker Compose not found. Is " + COMPOSE_EXECUTABLE + " on the PATH?");
        }

        final Map<String, String> environment = Maps.newHashMap(env);
        environment.put(ENV_PROJECT_NAME, identifier);

        final File dockerComposeBaseFile = composeFiles.get(0);
        final File pwd = dockerComposeBaseFile.getAbsoluteFile().getParentFile().getAbsoluteFile();
        environment.put(ENV_COMPOSE_FILE, new File(pwd, dockerComposeBaseFile.getAbsoluteFile().getName()).getAbsolutePath());

        logger().info("Local Docker Compose is running command: {}", cmd);

        final List<String> command = Splitter.onPattern(" ")
                .omitEmptyStrings()
                .splitToList(COMPOSE_EXECUTABLE + " " + cmd);

        try {
            new ProcessExecutor().command(command)
                    .redirectOutput(Slf4jStream.of(logger()).asInfo())
                    .redirectError(Slf4jStream.of(logger()).asError())
                    .environment(environment)
                    .directory(pwd)
                    .exitValueNormal()
                    .executeNoTimeout();

            logger().info("Docker Compose has finished running");

        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker Compose exited abnormally with code " +
                    e.getExitValue() + " whilst running command: " + cmd);

        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker Compose command: " + cmd, e);
        }
    }

    /**
     * @return a logger
     */
    private Logger logger() {
        return DockerLoggerFactory.getLogger(COMPOSE_EXECUTABLE);
    }
}
