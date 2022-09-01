package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testcontainers.utility.LogUtils;
import org.testcontainers.utility.ResourceReaper;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Testcontainers implementation for Docker Compose V2. <br>
 * It uses docker binary.
 */
@Slf4j
public class ComposeContainer<SELF extends ComposeContainer<SELF>>
    extends FailureDetectingExternalResource
    implements Startable {

    /**
     * Random identifier which will become part of spawned containers names, so we can shut them down
     */
    private final String identifier;

    private final List<File> composeFiles;

    private DockerComposeFiles dockerComposeFiles;

    private final Map<String, Integer> scalingPreferences = new HashMap<>();

    private DockerClient dockerClient;

    private boolean localCompose;

    private boolean pull = true;

    private boolean build = false;

    private Set<String> options = new HashSet<>();

    private boolean tailChildContainers;

    private String project;

    private final AtomicInteger nextAmbassadorPort = new AtomicInteger(2000);

    private final Map<String, Map<Integer, Integer>> ambassadorPortMappings = new ConcurrentHashMap<>();

    private final Map<String, ComposeServiceWaitStrategyTarget> serviceInstanceMap = new ConcurrentHashMap<>();

    private final Map<String, WaitAllStrategy> waitStrategyMap = new ConcurrentHashMap<>();

    private Duration startupTimeout = Duration.ofMinutes(30);

    private final SocatContainer ambassadorContainer = new SocatContainer();

    private final Map<String, List<Consumer<OutputFrame>>> logConsumers = new ConcurrentHashMap<>();

    private static final Object MUTEX = new Object();

    private List<String> services = new ArrayList<>();

    private Set<ExposedService> exposedServices = new HashSet<>();

    private static final String DOCKER_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker.exe" : "docker";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("docker:20.10.17");

    @Value
    @EqualsAndHashCode(of = { "name", "port" })
    class ExposedService {

        String name;

        int port;

        WaitStrategy waitStrategy;
    }

    /**
     * Properties that should be passed through to all Compose and ambassador containers (not
     * necessarily to containers that are spawned by Compose itself)
     */
    private Map<String, String> env = new HashMap<>();

    private ComposeConfiguration.RemoveImages removeImages;

    public ComposeContainer(File... composeFiles) {
        this(Arrays.asList(composeFiles));
    }

    public ComposeContainer(List<File> composeFiles) {
        this(Base58.randomString(6).toLowerCase(), composeFiles);
    }

    public ComposeContainer(String identifier, File... composeFiles) {
        this(identifier, Arrays.asList(composeFiles));
    }

    public ComposeContainer(String identifier, List<File> composeFiles) {
        this.composeFiles = composeFiles;
        this.dockerComposeFiles = new DockerComposeFiles(composeFiles);

        // Use a unique identifier so that containers created for this compose environment can be identified
        this.identifier = identifier;
        this.project = randomProjectId();

        this.dockerClient = DockerClientFactory.lazyClient();
    }

    @Override
    @Deprecated
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    @Override
    @Deprecated
    public void starting(Description description) {
        start();
    }

    @Override
    @Deprecated
    protected void succeeded(Description description) {}

    @Override
    @Deprecated
    protected void failed(Throwable e, Description description) {}

    @Override
    @Deprecated
    public void finished(Description description) {
        stop();
    }

    @Override
    public void start() {
        synchronized (MUTEX) {
            registerContainersForShutdown();
            if (pull) {
                try {
                    pullImages();
                } catch (ContainerLaunchException e) {
                    log.warn("Exception while pulling images, using local images if available", e);
                }
            }
            registerServices();
            createServices();
            startAmbassadorContainers();
            waitUntilServiceStarted();
        }
    }

    private void registerServices() {
        for (ExposedService exposedService : this.exposedServices) {
            String serviceInstanceName = getServiceInstanceName(exposedService.getName());

            /*
             * For every service/port pair that needs to be exposed, we register a target on an 'ambassador container'.
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

            // Ambassador container will be started together after docker compose has started
            int ambassadorPort = nextAmbassadorPort.getAndIncrement();
            ambassadorPortMappings
                .computeIfAbsent(serviceInstanceName, __ -> new ConcurrentHashMap<>())
                .put(exposedService.getPort(), ambassadorPort);
            ambassadorContainer.withTarget(ambassadorPort, serviceInstanceName, exposedService.getPort());
            ambassadorContainer.addLink(
                new FutureContainer(this.project + composeSeparator() + serviceInstanceName),
                serviceInstanceName
            );
            addWaitStrategy(serviceInstanceName, exposedService.getWaitStrategy());
        }
    }

    private void pullImages() {
        // Pull images using our docker client rather than compose itself,
        // (a) as a workaround for https://github.com/docker/compose/issues/5854, which prevents authenticated image pulls being possible when credential helpers are in use
        // (b) so that credential helper-based auth still works when compose is running from within a container
        dockerComposeFiles
            .getDependencyImages()
            .forEach(imageName -> {
                try {
                    log.info(
                        "Preemptively checking local images for '{}', referenced via a compose file or transitive Dockerfile. If not available, it will be pulled.",
                        imageName
                    );
                    new RemoteDockerImage(DockerImageName.parse(imageName))
                        .withImageNameSubstitutor(ImageNameSubstitutor.noop())
                        .get();
                } catch (Exception e) {
                    log.warn(
                        "Unable to pre-fetch an image ({}) depended upon by Docker Compose build - startup will continue but may fail. Exception message was: {}",
                        imageName,
                        e.getMessage()
                    );
                }
            });
    }

    public SELF withServices(@NonNull String... services) {
        this.services = Arrays.asList(services);
        return self();
    }

    private void createServices() {
        // services that have been explicitly requested to be started. If empty, all services should be started.
        final String serviceNameArgs = Stream
            .concat(
                services.stream(), // services that have been specified with `withServices`
                scalingPreferences.keySet().stream() // services that are implicitly needed via `withScaledService`
            )
            .distinct()
            .collect(Collectors.joining(" "));

        // Apply scaling for the services specified using `withScaledService`
        final String scalingOptions = scalingPreferences
            .entrySet()
            .stream()
            .map(entry -> "--scale " + entry.getKey() + "=" + entry.getValue())
            .distinct()
            .collect(Collectors.joining(" "));

        String command = getUpCommand(optionsAsString());

        if (build) {
            command += " --build";
        }

        if (!Strings.isNullOrEmpty(scalingOptions)) {
            command += " " + scalingOptions;
        }

        if (!Strings.isNullOrEmpty(serviceNameArgs)) {
            command += " " + serviceNameArgs;
        }

        // Run the docker-compose container, which starts up the services
        runWithCompose(command);
    }

    private String optionsAsString() {
        String optionsString = options.stream().collect(Collectors.joining(" "));
        if (optionsString.length() != 0) {
            // ensures that there is a space between the options and 'up' if options are passed.
            return optionsString;
        } else {
            // otherwise two spaces would appear between 'docker-compose' and 'up'
            return StringUtils.EMPTY;
        }
    }

    private void waitUntilServiceStarted() {
        listChildContainers().forEach(this::createServiceInstance);

        Set<String> servicesToWaitFor = waitStrategyMap.keySet();
        Set<String> instantiatedServices = serviceInstanceMap.keySet();
        Sets.SetView<String> missingServiceInstances = Sets.difference(servicesToWaitFor, instantiatedServices);

        if (!missingServiceInstances.isEmpty()) {
            throw new IllegalStateException(
                "Services named " +
                missingServiceInstances +
                " " +
                "do not exist, but wait conditions have been defined " +
                "for them. This might mean that you misspelled " +
                "the service name when defining the wait condition."
            );
        }

        serviceInstanceMap.forEach(this::waitUntilServiceStarted);
    }

    private void createServiceInstance(com.github.dockerjava.api.model.Container container) {
        String serviceName = getServiceNameFromContainer(container);
        final ComposeServiceWaitStrategyTarget containerInstance = new ComposeServiceWaitStrategyTarget(
            dockerClient,
            container,
            ambassadorContainer,
            ambassadorPortMappings.getOrDefault(serviceName, new HashMap<>())
        );

        String containerId = containerInstance.getContainerId();
        if (tailChildContainers) {
            followLogs(containerId, new Slf4jLogConsumer(log).withPrefix(container.getNames()[0]));
        }
        //follow logs using registered consumers for this service
        logConsumers
            .getOrDefault(serviceName, Collections.emptyList())
            .forEach(consumer -> followLogs(containerId, consumer));
        serviceInstanceMap.putIfAbsent(serviceName, containerInstance);
    }

    private void waitUntilServiceStarted(String serviceName, ComposeServiceWaitStrategyTarget serviceInstance) {
        final WaitAllStrategy waitAllStrategy = waitStrategyMap.get(serviceName);
        if (waitAllStrategy != null) {
            waitAllStrategy.waitUntilReady(serviceInstance);
        }
    }

    private String getServiceNameFromContainer(com.github.dockerjava.api.model.Container container) {
        final String containerName = container.getLabels().get("com.docker.compose.service");
        final String containerNumber = container.getLabels().get("com.docker.compose.container-number");
        return String.format("%s%s%s", containerName, composeSeparator(), containerNumber);
    }

    private void runWithCompose(String cmd) {
        Preconditions.checkNotNull(composeFiles);
        Preconditions.checkArgument(!composeFiles.isEmpty(), "No docker compose file have been provided");

        final DockerCompose dockerCompose;
        if (localCompose) {
            dockerCompose = new LocalDockerCompose(DOCKER_EXECUTABLE, composeFiles, project);
        } else {
            dockerCompose = new ContainerisedDockerCompose(DEFAULT_IMAGE_NAME, composeFiles, project);
        }

        dockerCompose.withCommand(cmd).withEnv(env).invoke();
    }

    private void registerContainersForShutdown() {
        ResourceReaper
            .instance()
            .registerLabelsFilterForCleanup(Collections.singletonMap("com.docker.compose.project", project));
    }

    @VisibleForTesting
    List<Container> listChildContainers() {
        return dockerClient
            .listContainersCmd()
            .withShowAll(true)
            .exec()
            .stream()
            .filter(container -> Arrays.stream(container.getNames()).anyMatch(name -> name.startsWith("/" + project)))
            .collect(Collectors.toList());
    }

    private void startAmbassadorContainers() {
        if (!ambassadorPortMappings.isEmpty()) {
            ambassadorContainer.start();
        }
    }

    @Override
    public void stop() {
        synchronized (MUTEX) {
            try {
                // shut down the ambassador container
                ambassadorContainer.stop();

                // Kill the services using docker-compose
                String cmd = getDownCommand();
                if (removeImages != null) {
                    cmd += " --rmi " + removeImages.dockerRemoveImagesType();
                }
                runWithCompose(cmd);
            } finally {
                project = randomProjectId();
            }
        }
    }

    public SELF withExposedService(String serviceName, int servicePort) {
        return withExposedService(serviceName, servicePort, Wait.defaultWaitStrategy());
    }

    public ComposeContainer withExposedService(String serviceName, int instance, int servicePort) {
        return withExposedService(serviceName + composeSeparator() + instance, servicePort);
    }

    public ComposeContainer withExposedService(
        String serviceName,
        int instance,
        int servicePort,
        WaitStrategy waitStrategy
    ) {
        return withExposedService(serviceName + composeSeparator() + instance, servicePort, waitStrategy);
    }

    public SELF withExposedService(String serviceName, int servicePort, @NonNull WaitStrategy waitStrategy) {
        this.exposedServices.add(new ExposedService(serviceName, servicePort, waitStrategy));
        return self();
    }

    private String getServiceInstanceName(String serviceName) {
        String serviceInstanceName = serviceName;
        String regex = String.format(".*%s[0-9]+", composeSeparator());
        if (!serviceInstanceName.matches(regex)) {
            serviceInstanceName += String.format("%s1", composeSeparator()); // implicit first instance of this service
        }
        return serviceInstanceName;
    }

    /*
     * can have multiple wait strategies for a single container, e.g. if waiting on several ports
     * if no wait strategy is defined, the WaitAllStrategy will return immediately.
     * The WaitAllStrategy uses the startup timeout for everything as a global maximum,
     * but we expect timeouts to be handled by the inner strategies.
     */
    private void addWaitStrategy(String serviceInstanceName, @NonNull WaitStrategy waitStrategy) {
        final WaitAllStrategy waitAllStrategy = waitStrategyMap.computeIfAbsent(
            serviceInstanceName,
            __ -> {
                return new WaitAllStrategy(WaitAllStrategy.Mode.WITH_MAXIMUM_OUTER_TIMEOUT)
                    .withStartupTimeout(startupTimeout);
            }
        );
        waitAllStrategy.withStrategy(waitStrategy);
    }

    /**
     * Specify the {@link WaitStrategy} to use to determine if the container is ready.
     *
     * @param serviceName  the name of the service to wait for
     * @param waitStrategy the WaitStrategy to use
     * @return this
     * @see org.testcontainers.containers.wait.strategy.Wait#defaultWaitStrategy()
     */
    public SELF waitingFor(String serviceName, @NonNull WaitStrategy waitStrategy) {
        String serviceInstanceName = getServiceInstanceName(serviceName);
        addWaitStrategy(serviceInstanceName, waitStrategy);
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
        return ambassadorContainer.getHost();
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
        Map<Integer, Integer> portMap = ambassadorPortMappings.get(getServiceInstanceName(serviceName));

        if (portMap == null) {
            throw new IllegalArgumentException(
                "Could not get a port for '" +
                serviceName +
                "'. " +
                "Testcontainers does not have an exposed port configured for '" +
                serviceName +
                "'. " +
                "To fix, please ensure that the service '" +
                serviceName +
                "' has ports exposed using .withExposedService(...)"
            );
        } else {
            return ambassadorContainer.getMappedPort(portMap.get(servicePort));
        }
    }

    private String getUpCommand(String options) {
        if (options != null && !options.isEmpty()) {
            String cmd = "compose %s up -d";
            return String.format(cmd, options);
        }
        return "compose up -d";
    }

    private String getDownCommand() {
        return "compose down -v";
    }

    private String composeSeparator() {
        return "-";
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

    /**
     * Attach an output consumer at container startup, enabling stdout and stderr to be followed, waited on, etc.
     * <p>
     * More than one consumer may be registered.
     *
     * @param serviceName the name of the service as set in the docker-compose.yml file
     * @param consumer    consumer that output frames should be sent to
     * @return this instance, for chaining
     */
    public SELF withLogConsumer(String serviceName, Consumer<OutputFrame> consumer) {
        String serviceInstanceName = getServiceInstanceName(serviceName);
        final List<Consumer<OutputFrame>> consumers =
            this.logConsumers.getOrDefault(serviceInstanceName, new ArrayList<>());
        consumers.add(consumer);
        this.logConsumers.putIfAbsent(serviceInstanceName, consumers);
        return self();
    }

    /**
     * Whether to always build images before starting containers.
     *
     * @return this instance, for chaining
     */
    public SELF withBuild(boolean build) {
        this.build = build;
        return self();
    }

    /**
     * Adds options to the docker-compose command, e.g. docker-compose --compatibility.
     *
     * @return this instance, for chaining
     */
    public SELF withOptions(String... options) {
        this.options = new HashSet<>(Arrays.asList(options));
        return self();
    }

    /**
     * Remove images after containers shutdown.
     *
     * @return this instance, for chaining
     */
    public SELF withRemoveImages(ComposeConfiguration.RemoveImages removeImages) {
        this.removeImages = removeImages;
        return self();
    }

    /**
     * Set the maximum startup timeout all the waits set are bounded to.
     *
     * @return this instance. for chaining
     */
    public SELF withStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
        return self();
    }

    public Optional<ContainerState> getContainerByServiceName(String serviceName) {
        return Optional.ofNullable(serviceInstanceMap.get(serviceName));
    }

    private void followLogs(String containerId, Consumer<OutputFrame> consumer) {
        LogUtils.followOutput(dockerClient, containerId, consumer);
    }

    private SELF self() {
        return (SELF) this;
    }

    private String randomProjectId() {
        return identifier + Base58.randomString(6).toLowerCase();
    }
}
