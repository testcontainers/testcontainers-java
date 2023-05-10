package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.RemoteDockerImage;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class ComposeDelegate {

    private final ComposeVersion composeVersion;

    private final String composeSeparator;

    private final DockerClient dockerClient;

    private final List<File> composeFiles;

    private final DockerComposeFiles dockerComposeFiles;

    private final String identifier;

    @Getter
    private final String project;

    private final String executable;

    private final DockerImageName defaultImageName;

    private final AtomicInteger nextAmbassadorPort = new AtomicInteger(2000);

    private final Map<String, Map<Integer, Integer>> ambassadorPortMappings = new ConcurrentHashMap<>();

    private final Map<String, List<Consumer<OutputFrame>>> logConsumers = new ConcurrentHashMap<>();

    @Getter
    private final SocatContainer ambassadorContainer = new SocatContainer();

    private final Map<String, ComposeServiceWaitStrategyTarget> serviceInstanceMap = new ConcurrentHashMap<>();

    private final Map<String, WaitAllStrategy> waitStrategyMap = new ConcurrentHashMap<>();

    @Setter
    private Duration startupTimeout = Duration.ofMinutes(30);

    ComposeDelegate(
        ComposeVersion composeVersion,
        List<File> composeFiles,
        String identifier,
        String executable,
        DockerImageName defaultImageName
    ) {
        this.composeVersion = composeVersion;
        this.composeSeparator = composeVersion.getSeparator();
        this.dockerClient = DockerClientFactory.lazyClient();
        this.composeFiles = composeFiles;
        this.dockerComposeFiles = new DockerComposeFiles(this.composeFiles);
        this.identifier = identifier.toLowerCase();
        this.project = randomProjectId();
        this.executable = executable;
        this.defaultImageName = defaultImageName;
    }

    void pullImages() {
        // Pull images using our docker client rather than compose itself,
        // (a) as a workaround for https://github.com/docker/compose/issues/5854, which prevents authenticated image pulls being possible when credential helpers are in use
        // (b) so that credential helper-based auth still works when compose is running from within a container
        this.dockerComposeFiles.getDependencyImages()
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

    void createServices(
        boolean localCompose,
        boolean build,
        final Set<String> options,
        final List<String> services,
        final Map<String, Integer> scalingPreferences,
        Map<String, String> env
    ) {
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

        String command = getUpCommand(optionsAsString(options));

        if (build) {
            command += " --build";
        }

        if (!Strings.isNullOrEmpty(scalingOptions)) {
            command += " " + scalingOptions;
        }

        if (!Strings.isNullOrEmpty(serviceNameArgs)) {
            command += " " + serviceNameArgs;
        }

        // Run the docker compose container, which starts up the services
        runWithCompose(localCompose, command, env);
    }

    private String getUpCommand(String options) {
        if (options == null || options.equals("")) {
            return this.composeVersion == ComposeVersion.V1 ? "up -d" : "compose up -d";
        }
        String cmd = this.composeVersion == ComposeVersion.V1 ? "%s up -d" : "compose %s up -d";
        return String.format(cmd, options);
    }

    private String optionsAsString(final Set<String> options) {
        String optionsString = options.stream().collect(Collectors.joining(" "));
        if (optionsString.length() != 0) {
            // ensures that there is a space between the options and 'up' if options are passed.
            return optionsString;
        } else {
            // otherwise two spaces would appear between 'docker-compose' and 'up'
            return StringUtils.EMPTY;
        }
    }

    void waitUntilServiceStarted(boolean tailChildContainers) {
        listChildContainers().forEach(container -> createServiceInstance(container, tailChildContainers));

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

    private void createServiceInstance(Container container, boolean tailChildContainers) {
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
        return String.format("%s%s%s", containerName, this.composeSeparator, containerNumber);
    }

    public void runWithCompose(boolean localCompose, String cmd) {
        runWithCompose(localCompose, cmd, Collections.emptyMap());
    }

    public void runWithCompose(boolean localCompose, String cmd, Map<String, String> env) {
        Preconditions.checkNotNull(composeFiles);
        Preconditions.checkArgument(!composeFiles.isEmpty(), "No docker compose file have been provided");

        final DockerCompose dockerCompose;
        if (localCompose) {
            dockerCompose = new LocalDockerCompose(this.executable, composeFiles, project);
        } else {
            dockerCompose = new ContainerisedDockerCompose(this.defaultImageName, composeFiles, project);
        }

        dockerCompose.withCommand(cmd).withEnv(env).invoke();
    }

    void registerContainersForShutdown() {
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

    void startAmbassadorContainer() {
        if (!this.ambassadorPortMappings.isEmpty()) {
            this.ambassadorContainer.start();
        }
    }

    public void withExposedService(String serviceName, int servicePort) {
        withExposedService(serviceName, servicePort, Wait.defaultWaitStrategy());
    }

    public void withExposedService(String serviceName, int instance, int servicePort) {
        withExposedService(serviceName + this.composeSeparator + instance, servicePort);
    }

    public void withExposedService(String serviceName, int instance, int servicePort, WaitStrategy waitStrategy) {
        withExposedService(serviceName + this.composeSeparator + instance, servicePort, waitStrategy);
    }

    public void withExposedService(String serviceName, int servicePort, @NonNull WaitStrategy waitStrategy) {
        String serviceInstanceName = getServiceInstanceName(serviceName);

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
            .put(servicePort, ambassadorPort);
        ambassadorContainer.withTarget(ambassadorPort, serviceInstanceName, servicePort);
        ambassadorContainer.addLink(
            new FutureContainer(this.project + this.composeSeparator + serviceInstanceName),
            serviceInstanceName
        );
        addWaitStrategy(serviceInstanceName, waitStrategy);
    }

    String getServiceInstanceName(String serviceName) {
        String serviceInstanceName = serviceName;
        String regex = String.format(".*%s[0-9]+", this.composeSeparator);
        if (!serviceInstanceName.matches(regex)) {
            serviceInstanceName += String.format("%s1", this.composeSeparator); // implicit first instance of this service
        }
        return serviceInstanceName;
    }

    /*
     * can have multiple wait strategies for a single container, e.g. if waiting on several ports
     * if no wait strategy is defined, the WaitAllStrategy will return immediately.
     * The WaitAllStrategy uses the startup timeout for everything as a global maximum, but we expect timeouts to be handled by the inner strategies.
     */
    void addWaitStrategy(String serviceInstanceName, @NonNull WaitStrategy waitStrategy) {
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
        Map<Integer, Integer> portMap = this.ambassadorPortMappings.get(getServiceInstanceName(serviceName));

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

    Optional<ContainerState> getContainerByServiceName(String serviceName) {
        String serviceInstantName = getServiceInstanceName(serviceName);
        return Optional.ofNullable(serviceInstanceMap.get(serviceInstantName));
    }

    private void followLogs(String containerId, Consumer<OutputFrame> consumer) {
        LogUtils.followOutput(dockerClient, containerId, consumer);
    }

    String randomProjectId() {
        return this.identifier + Base58.randomString(6).toLowerCase();
    }

    void withLogConsumer(String serviceName, Consumer<OutputFrame> consumer) {
        String serviceInstanceName = getServiceInstanceName(serviceName);
        final List<Consumer<OutputFrame>> consumers =
            this.logConsumers.getOrDefault(serviceInstanceName, new ArrayList<>());
        consumers.add(consumer);
        this.logConsumers.putIfAbsent(serviceInstanceName, consumers);
    }

    String getServiceHost() {
        return this.ambassadorContainer.getHost();
    }

    enum ComposeVersion {
        V1("_"),

        V2("-");

        private final String separator;

        ComposeVersion(String separator) {
            this.separator = separator;
        }

        public String getSeparator() {
            return this.separator;
        }
    }
}
