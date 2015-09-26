package org.testcontainers.containers;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.testcontainers.SingletonDockerClient;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.utility.PathOperations;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for that allows a container to be launched and controlled.
 */
public class GenericContainer extends TestWatcher implements LinkableContainer {

    protected String dockerImageName = "alpine:3.2";
    protected String containerId;
    protected String containerName;
    protected DockerClient dockerClient = SingletonDockerClient.instance().client();;
    protected String tag = "latest";
    protected Map<String, List<PortBinding>> ports;
    private boolean normalTermination = false;

    private static final Set<String> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();
    private List<String> exposedPorts = new ArrayList<>();
    private List<String> env = new ArrayList<>();
    private String[] commandParts;
    private List<String> binds = new ArrayList<>();
    private List<String> links = new ArrayList<>();

    private static final RateLimiter DOCKER_CLIENT_RATE_LIMITER = RateLimiterBuilder
            .newBuilder()
            .withRate(1, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    public GenericContainer() {

    }

    public GenericContainer(final String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }



    /**
     * Starts the container using docker, pulling an image if necessary.
     */
    public void start() {

        Profiler profiler = new Profiler("Container startup");
        profiler.setLogger(logger());

        logger().debug("Starting container: {}", getDockerImageName());

        try {
            pullImageIfNeeded(getDockerImageName(), profiler.startNested("Pull image if needed"));

            profiler.start("Prepare container configuration and host configuration");
            ContainerConfig containerConfig = getContainerConfig();

            logger().info("Creating container for image: {}", getDockerImageName());
            profiler.start("Create container");
            ContainerCreation containerCreation = dockerClient.createContainer(containerConfig);
            containerId = containerCreation.id();

            logger().info("Starting container with ID: {}", containerId);
            profiler.start("Start container");
            dockerClient.startContainer(containerId);

            profiler.start("Inspecting container");
            ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
            containerName = containerInfo.name();

            // Wait until the container is starting
            profiler.start("Wait until container state=running");
            Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, () -> {
                return DOCKER_CLIENT_RATE_LIMITER.getWhenReady(() -> {
                    return dockerClient.inspectContainer(containerId).state().running();
                });
            });

            // Tell subclasses that we're starting
            logger().info("Container is starting with port mapping: {}", dockerClient.inspectContainer(containerId).networkSettings().ports());
            profiler.start("Call containerIsStarting on subclasses");
            containerIsStarting(containerInfo);

            profiler.start("Wait until container started");
            waitUntilContainerStarted();

            logger().info("Container {} started", getDockerImageName());

            profiler.start("Set up shutdown hooks");
            // If the container stops before the after() method, its termination was unexpected
            Executors.newSingleThreadExecutor().submit(() -> {
                Exception caughtException = null;
                try {
                    dockerClient.waitContainer(containerId);
                } catch (DockerException | InterruptedException e) {
                    caughtException = e;
                }

                if (!normalTermination) {
                    throw new RuntimeException("Container exited unexpectedly", caughtException);
                }
            });

            // If the JVM stops without the container being stopped, try and stop the container
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger().debug("Hit shutdown hook for container {}", GenericContainer.this.containerId);
                GenericContainer.this.stop();
            }));

        } catch (Exception e) {
            logger().error("Could not start container", e);

            throw new ContainerLaunchException("Could not create/start container", e);
        } finally {
            profiler.stop().log();
        }
    }

    /**
     * Provide a logger that references the docker image name.
     *
     * @return
     */
    protected Logger logger() {
        return LoggerFactory.getLogger("testcontainers[" + getDockerImageName() + "]");
    }

    private void pullImageIfNeeded(final String imageName, Profiler profiler) throws DockerException, InterruptedException {

        if (AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
            return;
        }

        profiler.start("Check local images");
        List<Image> images = dockerClient.listImages(DockerClient.ListImagesParam.create("name", getDockerImageName()));
        for (Image image : images) {
            if (image.repoTags().contains(imageName)) {
                // the image exists
                AVAILABLE_IMAGE_NAME_CACHE.add(imageName);
                return;
            }
        }

        logger().info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);
        profiler.start("Pull image");
        dockerClient.pull(getDockerImageName(), message -> {
            if (message.error() != null) {
                if (message.error().contains("404") || message.error().contains("not found")) {
                    throw new ImageNotFoundException(imageName, message.toString());
                } else {
                    throw new ImagePullFailedException(imageName, message.toString());
                }
            }
        });

        AVAILABLE_IMAGE_NAME_CACHE.add(imageName);
    }

    /**
     * Stops the container.
     */
    public void stop() {

        try {
            logger().trace("Stopping container: {}", containerId);
            normalTermination = true;
            dockerClient.killContainer(containerId);
            dockerClient.removeContainer(containerId, true);
            logger().info("Stopped and removed container: {}", getDockerImageName());
        } catch (DockerException | InterruptedException e) {
            logger().trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
        }
    }

    /**
     * Creates a directory on the local filesystem which will be mounted as a volume for the container.
     *
     * @param temporary is the volume directory temporary? If true, the directory will be deleted on JVM shutdown.
     * @return path to the volume directory
     * @throws IOException
     */
    protected Path createVolumeDirectory(boolean temporary) throws IOException {
        File file = new File(".tmp-volume-" + System.currentTimeMillis());
        file.mkdirs();
        final Path directory = file.toPath();

        if (temporary) Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                PathOperations.recursiveDeleteDir(directory);
            }
        }));

        return directory;
    }

    protected void containerIsStarting(ContainerInfo containerInfo) {
        ports = containerInfo.networkSettings().ports();
    }

    protected String getLivenessCheckPort() {
        if (exposedPorts.size() > 0) {
            return getMappedPort(exposedPorts.get(0));
        } else {
            return null;
        }
    }

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

        builder.hostConfig(HostConfig.builder().binds(binds).links(links).publishAllPorts(true).build());

        return builder
                .build();
    }

    protected String getDockerImageName() {
        return dockerImageName;
    }

    /**
     * Wait until the container has started. The default implementation simply
     * waits for a port to start listening; subclasses may override if more
     * sophisticated behaviour is required.
     */
    protected void waitUntilContainerStarted() {
        waitForListeningPort(SingletonDockerClient.instance().dockerHostIpAddress(), getLivenessCheckPort());
    }

    /**
     * Waits for a port to start listening for incoming connections.
     *
     * @param ipAddress the IP address to attempt to connect to
     * @param port      the port which will start accepting connections
     */
    protected void waitForListeningPort(String ipAddress, String port) {

        if (port == null) {
            return;
        }

        try {
            Unreliables.retryUntilSuccess(60, TimeUnit.SECONDS, () -> {
                DOCKER_CLIENT_RATE_LIMITER.doWhenReady(() -> {
                    try {
                        new Socket(ipAddress, Integer.valueOf(port)).close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" + ipAddress + ":" + port + " should be listening)");
        }
    }

    protected void checkContainerNotAborted() throws DockerException, InterruptedException {
        ContainerState state = dockerClient.inspectContainer(containerId).state();
        if (!state.running()) {
            throw new ContainerLaunchException("Container failed to start, and exited with exit code: " + state.exitCode());
        }
    }

    public void setTag(String tag) {
        this.tag = tag != null ? tag : "latest";
    }

    public String getContainerName() {
        return containerName;
    }

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     */
    public String getIpAddress() {
        return SingletonDockerClient.instance().dockerHostIpAddress();
    }

    public Boolean isRunning() {
        try {
            return dockerClient.inspectContainer(containerId).state().running();
        } catch (DockerException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Set the ports that this container listens on
     *
     * @param exposedPorts a list of ports in 'number/protocol' format, e.g. '80/tcp'
     */
    public void setExposedPorts(List<String> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    /**
     * Set the command that should be run in the container
     *
     * @param command a command in single string format (will automatically be split on spaces)
     */
    public void setCommand(String command) {
        this.commandParts = command.split(" ");
    }

    /**
     * Set the command that should be run in the container
     *
     * @param commandParts a command as an array of string parts
     */
    public void setCommand(String[] commandParts) {
        this.commandParts = commandParts;
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
     *
     * @param key   environment variable key
     * @param value environment variable value
     */
    public void addEnv(String key, String value) {
        env.add(key + "=" + value);
    }

    public void addFileSystemBind(String hostPath, String containerPath, GenericContainer.BindMode mode) {
        binds.add(hostPath + ":" + containerPath + ":" + mode.shortForm);
    }

    public void addLink(String otherContainerName, String alias) {
        links.add(otherContainerName + ":" + alias);
    }

    public void addExposedPort(String port) {
        exposedPorts.add(port);
    }

    public enum BindMode {
        READ_ONLY("ro"), READ_WRITE("rw");

        public final String shortForm;

        BindMode(String shortForm) {
            this.shortForm = shortForm;
        }
    }

    @Override
    protected void starting(Description description) {
        this.start();
    }

    @Override
    protected void finished(Description description) {
        this.stop();
    }


    public GenericContainer withImageName(String imageName) {
        this.dockerImageName = imageName;
        return this;
    }

    /**
     * Set the ports that this container listens on
     *
     * @param ports an array of TCP ports
     * @return this
     */
    public GenericContainer withExposedPorts(int... ports) {
        String[] stringValues = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            stringValues[i] = String.valueOf(ports[i]);
        }

        return withExposedPorts(stringValues);
    }

    /**
     * Set the ports that this container listens on
     *
     * @param ports an array of ports in either 'port/protocol' format (e.g. '80/tcp') or 'port' format (e.g. '80')
     * @return this
     */
    public GenericContainer withExposedPorts(String... ports) {
        List<String> portsWithSuffix = new ArrayList<>();

        for (String rawPort : ports) {
            if (rawPort.contains("/")) {
                portsWithSuffix.add(rawPort);
            } else {
                portsWithSuffix.add(rawPort + "/tcp");
            }
        }

        this.setExposedPorts(portsWithSuffix);
        return this;
    }

    /**
     * Add an environment variable to be passed to the container.
     *
     * @param key   environment variable key
     * @param value environment variable value
     * @return this
     */
    public GenericContainer withEnv(String key, String value) {
        this.addEnv(key, value);
        return this;
    }

    /**
     * Set the command that should be run in the container
     *
     * @param cmd a command in single string format (will automatically be split on spaces)
     * @return this
     */
    public GenericContainer withCommand(String cmd) {
        this.setCommand(cmd);
        return this;
    }

    /**
     * Set the command that should be run in the container
     *
     * @param commandParts a command as an array of string parts
     * @return this
     */
    public GenericContainer withCommand(String... commandParts) {
        this.setCommand(commandParts);
        return this;
    }

    /**
     * Map a resource (file or directory) on the classpath to a path inside the container
     *
     * @param resourcePath  path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
     * @param containerPath path this should be mapped to inside the container
     * @param mode          access mode for the file
     * @return this
     */
    public GenericContainer withClasspathResourceMapping(String resourcePath, String containerPath, GenericContainer.BindMode mode) {
        URL resource = GenericContainer.class.getClassLoader().getResource(resourcePath);

        if (resource == null) {
            throw new IllegalArgumentException("Could not find classpath resource at provided path: " + resourcePath);
        }
        String resourceFilePath = resource.getFile();

        this.addFileSystemBind(resourceFilePath, containerPath, mode);

        return this;
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    public String getMappedPort(int originalPort) {
        return this.getMappedPort(originalPort + "/tcp");
    }
}
