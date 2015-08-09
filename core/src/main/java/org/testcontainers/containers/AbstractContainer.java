package org.testcontainers.containers;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.SingletonDockerClient;
import org.testcontainers.utility.PathOperations;
import org.testcontainers.utility.Retryables;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base class for that allows a container to be launched and controlled.
 */
public abstract class AbstractContainer {

    protected String containerId;
    protected String containerName;
    protected DockerClient dockerClient;
    protected String tag = "latest";
    private boolean normalTermination = false;

    /**
     * Starts the container using docker, pulling an image if necessary.
     */
    public void start() {

        logger().debug("Start for container ({}): {}", getDockerImageName(), this);

        try {

            dockerClient = SingletonDockerClient.instance().client();

            pullImageIfNeeded(getDockerImageName());

            ContainerConfig containerConfig = getContainerConfig();

            HostConfig.Builder hostConfigBuilder = HostConfig.builder()
                    .publishAllPorts(true);
            customizeHostConfigBuilder(hostConfigBuilder);
            HostConfig hostConfig = hostConfigBuilder.build();

            logger().info("Creating container for image: {}", getDockerImageName());
            ContainerCreation containerCreation = dockerClient.createContainer(containerConfig);

            containerId = containerCreation.id();
            dockerClient.startContainer(containerId, hostConfig);
            logger().info("Starting container with ID: {}", containerId);

            ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);
            containerName = containerInfo.name();

            // Wait until the container is starting
            Retryables.retryUntilTrue(5, TimeUnit.SECONDS, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return dockerClient.inspectContainer(containerId).state().running();
                }
            });

            // Tell subclasses that we're starting
            logger().info("Container is starting with port mapping: {}", dockerClient.inspectContainer(containerId).networkSettings().ports());
            containerIsStarting(containerInfo);

            waitUntilContainerStarted();
            logger().info("Container started");

            // If the container stops before the after() method, its termination was unexpected
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    Exception caughtException = null;
                    try {
                        dockerClient.waitContainer(containerId);
                    } catch (DockerException | InterruptedException e) {
                        caughtException = e;
                    }

                    if (!normalTermination) {
                        throw new RuntimeException("Container exited unexpectedly", caughtException);
                    }
                }
            });

            // If the JVM stops without the container being stopped, try and stop the container
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    logger().debug("Hit shutdown hook for container {}", AbstractContainer.this.containerId);
                    AbstractContainer.this.stop();
                }
            }));
        } catch (Exception e) {
            logger().error("Could not start container", e);

            throw new ContainerLaunchException("Could not create/start container", e);
        }
    }

    /**
     * Provide a logger that references the docker image name.
     * @return
     */
    protected Logger logger() {
        return LoggerFactory.getLogger("testcontainers[" + getDockerImageName() + "]");
    }

    /**
     * Allows subclasses to apply additional configuration to the HostConfig.Builder prior to container creation.
     *
     * @param hostConfigBuilder
     */
    protected void customizeHostConfigBuilder(HostConfig.Builder hostConfigBuilder) {

    }

    private void pullImageIfNeeded(final String imageName) throws DockerException, InterruptedException {
        List<Image> images = dockerClient.listImages(DockerClient.ListImagesParam.create("name", getDockerImageName()));
        for (Image image : images) {
            if (image.repoTags().contains(imageName)) {
                // the image exists
                return;
            }
        }

        logger().info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);
        dockerClient.pull(getDockerImageName(), new ProgressHandler() {
            @Override
            public void progress(ProgressMessage message) throws DockerException {
                if (message.error() != null) {
                    if (message.error().contains("404") || message.error().contains("not found")) {
                        throw new ImageNotFoundException(imageName, message.toString());
                    } else {
                        throw new ImagePullFailedException(imageName, message.toString());
                    }
                }
            }
        });
    }

    /**
     * Stops the container.
     */
    public void stop() {

        logger().debug("Stop for container ({}): {}", getDockerImageName(), this);

        try {
            logger().info("Stopping container: {}", containerId);
            normalTermination = true;
            dockerClient.killContainer(containerId);
            dockerClient.removeContainer(containerId, true);
        } catch (DockerException | InterruptedException e) {
            logger().debug("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
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

    /**
     * Hook to notify subclasses that the container is starting
     *
     * @param containerInfo
     */
    protected abstract void containerIsStarting(ContainerInfo containerInfo);

    /**
     * @return a port number (specified as a String) which the contained application will listen on when alive. If a subclass does not need a liveness check this should just return null
     */
    protected abstract String getLivenessCheckPort();

    /**
     * @return container configuration
     */
    protected abstract ContainerConfig getContainerConfig();

    /**
     * @return the docker image name
     */
    protected abstract String getDockerImageName();

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

        for (int i = 0; i < 6000; i++) {
            try {

                checkContainerNotAborted();

                new Socket(ipAddress, Integer.valueOf(port)).close();
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {
                }
            } catch (InterruptedException | DockerException e) {
                throw new ContainerLaunchException("Container failed to start", e);
            }
        }
        throw new ContainerLaunchException("Timed out waiting for container port to open (" + ipAddress + ":" + port + " should be listening)");
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
     * @return an IP address
     */
    public String getIpAddress() {
        return SingletonDockerClient.instance().dockerHostIpAddress();
    }
}
