package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LogUtils;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Container which launches Docker Compose, for the purposes of launching a defined set of containers.
 */
@Slf4j
public class DockerComposeContainer<SELF extends DockerComposeContainer<SELF>>
    extends FailureDetectingExternalResource
    implements Startable {

    private final Map<String, Integer> scalingPreferences = new HashMap<>();

    private DockerClient dockerClient;

    private boolean localCompose;

    private boolean pull = true;

    private boolean build = false;

    private Set<String> options = new HashSet<>();

    private boolean tailChildContainers;

    private static final Object MUTEX = new Object();

    private List<String> services = new ArrayList<>();

    /**
     * Properties that should be passed through to all Compose and ambassador containers (not
     * necessarily to containers that are spawned by Compose itself)
     */
    private Map<String, String> env = new HashMap<>();

    private RemoveImages removeImages;

    public static final String COMPOSE_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker-compose.exe" : "docker-compose";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("docker/compose:1.29.2");

    private ComposeConfiguration<SELF> composeConfiguration;

    private String project;

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
        this.dockerClient = DockerClientFactory.lazyClient();
        this.composeConfiguration =
            new ComposeConfiguration<>(composeFiles, identifier, COMPOSE_EXECUTABLE, DEFAULT_IMAGE_NAME);
        this.project = this.composeConfiguration.getProject();
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
            this.composeConfiguration.registerContainersForShutdown();
            if (pull) {
                try {
                    this.composeConfiguration.pullImages();
                } catch (ContainerLaunchException e) {
                    log.warn("Exception while pulling images, using local images if available", e);
                }
            }
            this.composeConfiguration.createServices(
                    this.localCompose,
                    this.build,
                    this.options,
                    this.services,
                    this.scalingPreferences,
                    this.env
                );
            this.composeConfiguration.startAmbassadorContainer();
            this.composeConfiguration.waitUntilServiceStarted(this.tailChildContainers);
        }
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

    public SELF withServices(@NonNull String... services) {
        this.services = Arrays.asList(services);
        return self();
    }

    @Override
    public void stop() {
        synchronized (MUTEX) {
            try {
                this.composeConfiguration.getAmbassadorContainer().stop();

                // Kill the services using docker-compose
                String cmd = "down -v";
                if (removeImages != null) {
                    cmd += " --rmi " + removeImages.dockerRemoveImagesType();
                }
                this.composeConfiguration.runWithCompose(this.localCompose, cmd);
            } finally {
                this.project = this.composeConfiguration.randomProjectId();
            }
        }
    }

    public SELF withExposedService(String serviceName, int servicePort) {
        return this.composeConfiguration.withExposedService(serviceName, servicePort, Wait.defaultWaitStrategy());
    }

    public DockerComposeContainer withExposedService(String serviceName, int instance, int servicePort) {
        return withExposedService(serviceName + "_" + instance, servicePort);
    }

    public DockerComposeContainer withExposedService(
        String serviceName,
        int instance,
        int servicePort,
        WaitStrategy waitStrategy
    ) {
        return this.composeConfiguration.withExposedService(serviceName + "_" + instance, servicePort, waitStrategy);
    }

    public SELF withExposedService(String serviceName, int servicePort, @NonNull WaitStrategy waitStrategy) {
        this.composeConfiguration.withExposedService(serviceName, servicePort, waitStrategy);
        return self();
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
        String serviceInstanceName = this.composeConfiguration.getServiceInstanceName(serviceName);
        this.composeConfiguration.addWaitStrategy(serviceInstanceName, waitStrategy);
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
        return this.composeConfiguration.getServiceHost();
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
        return this.composeConfiguration.getServicePort(serviceName, servicePort);
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
        return this.composeConfiguration.withLogConsumer(serviceName, consumer);
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
    public SELF withRemoveImages(RemoveImages removeImages) {
        this.removeImages = removeImages;
        return self();
    }

    /**
     * Set the maximum startup timeout all the waits set are bounded to.
     *
     * @return this instance. for chaining
     */
    public SELF withStartupTimeout(Duration startupTimeout) {
        this.composeConfiguration.setStartupTimeout(startupTimeout);
        return self();
    }

    public Optional<ContainerState> getContainerByServiceName(String serviceName) {
        return this.composeConfiguration.getContainerByServiceName(serviceName);
    }

    private void followLogs(String containerId, Consumer<OutputFrame> consumer) {
        LogUtils.followOutput(this.dockerClient, containerId, consumer);
    }

    private SELF self() {
        return (SELF) this;
    }

    public enum RemoveImages {
        /**
         * Remove all images used by any service.
         */
        ALL("all"),

        /**
         * Remove only images that don't have a custom tag set by the `image` field.
         */
        LOCAL("local");

        private final String dockerRemoveImagesType;

        RemoveImages(final String dockerRemoveImagesType) {
            this.dockerRemoveImagesType = dockerRemoveImagesType;
        }

        public String dockerRemoveImagesType() {
            return dockerRemoveImagesType;
        }
    }
}
