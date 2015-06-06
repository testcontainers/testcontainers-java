package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * @author richardnorth
 */
public abstract class AbstractContainer implements ManagedContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger("TestContainer");

    protected String dockerHostIpAddress;
    private String containerId;
    private DockerClient dockerClient;
    private boolean normalTermination = false;
    protected String tag = "latest";

    public AbstractContainer() {
        try {

            DefaultDockerClient.Builder builder = DefaultDockerClient.builder();

            customizeBuilderForOs(builder);

            dockerClient = builder.build();
        } catch (Exception e) {
            LOGGER.error("Could not start container", e);
        }
    }

    public void start() {

        try {
            pullImageIfNeeded(tag);

            ContainerConfig containerConfig = getContainerConfig();

            HostConfig.Builder hostConfigBuilder = HostConfig.builder()
                    .publishAllPorts(true);
            customizeHostConfigBuilder(hostConfigBuilder);
            HostConfig hostConfig = hostConfigBuilder.build();

            LOGGER.info("Creating container for image: {}", getDockerImageName(tag));
            ContainerCreation containerCreation = dockerClient.createContainer(containerConfig);

            containerId = containerCreation.id();
            dockerClient.startContainer(containerId, hostConfig);
            LOGGER.info("Starting container with ID: {}", containerId);

            ContainerInfo containerInfo = dockerClient.inspectContainer(containerId);

            containerIsStarting(containerInfo);

            waitForListeningPort(dockerHostIpAddress, getLivenessCheckPort());
            LOGGER.info("Container started");

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
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (Exception e) {
            LOGGER.error("Could not start container", e);
        }
    }

    protected void customizeHostConfigBuilder(HostConfig.Builder hostConfigBuilder) {

    }

    private void pullImageIfNeeded(String tag) throws DockerException, InterruptedException {
        String imageName = getDockerImageName(tag);
        if (doesLocalImageExist(imageName)) {
            return;
        }

        LOGGER.info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);
        dockerClient.pull(imageName, message -> {
            if (message.error() != null) {
                if (message.error().contains("404") || message.error().contains("not found")) {
                    throw new ImageNotFoundException(imageName, message.toString());
                } else {
                    throw new ImagePullFailedException(imageName, message.toString());
                }
            }
        });
    }

    private boolean doesLocalImageExist(String imageName) {
        List<Image> images = null;
        try {
            images = dockerClient.listImages(DockerClient.ListImagesParam.create("name", imageName));
        } catch (DockerException | InterruptedException e) {
            e.printStackTrace();
        }
        for (Image image : images) {
            if (image.repoTags().contains(imageName)) {
                // the image exists
                return true;
            }
        }
        return false;
    }

    public void stop() {
        try {
            LOGGER.info("Stopping container: {}", containerId);
            normalTermination = true;
            dockerClient.killContainer(containerId);
            dockerClient.removeContainer(containerId, true);
        } catch (DockerException | InterruptedException e) {
            LOGGER.debug("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped", containerId, e);
        }
    }

    protected abstract void containerIsStarting(ContainerInfo containerInfo);

    protected abstract String getLivenessCheckPort();

    protected abstract ContainerConfig getContainerConfig();

    /**
     * Wait until the container has started. The default implementation simply
     * waits for a port to start listening; subclasses may override if more
     * sophisticated behaviour is required.
     */
    protected void waitUntilContainerStarted() {
        waitForListeningPort(dockerHostIpAddress, getLivenessCheckPort());
    }

    protected void waitForListeningPort(String ipAddress, String port) {
        for (int i = 0; i < 6000; i++) {
            try {
                new Socket(ipAddress, Integer.valueOf(port));
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {
                }
            }
        }
        throw new IllegalStateException("Timed out waiting for container port to open (" + ipAddress + ":" + port + " should be listening)");
    }

    private void customizeBuilderForOs(DefaultDockerClient.Builder builder) throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // Running on a Mac therefore use boot2docker
            runShellCommand("/usr/local/bin/boot2docker", "up");
            dockerHostIpAddress = runShellCommand("/usr/local/bin/boot2docker", "ip");

            builder.uri("https://" + dockerHostIpAddress + ":2376")
                    .dockerCertificates(new DockerCertificates(Paths.get(System.getProperty("user.home") + "/.boot2docker/certs/boot2docker-vm")));
        } else {
            dockerHostIpAddress = "127.0.0.1";
        }
    }

    private String runShellCommand(String... command) throws IOException, InterruptedException, TimeoutException {
        ProcessResult result;
        result = new ProcessExecutor().command(command)
                .readOutput(true).execute();

        if (result.getExitValue() != 0) {
            System.err.println(result.getOutput().getString());
            throw new IllegalStateException();
        }
        return result.outputUTF8().trim();
    }

    public void setTag(String tag) {
        this.tag = tag != null ? tag : "latest";
    }

    public boolean hasExistingTag(String tagName) {
        return doesLocalImageExist(getDockerImageName(tagName));
    }

    @Override
    public void commitAndTag(String tagName) {
        try {
            dockerClient.commitContainer(containerId, this.getDockerImageName(), tagName, getContainerConfig(), "Tag created by TestContainers", "foo");
        } catch (DockerException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected abstract String getDockerImageName();

    protected String getDockerImageName(String tagName) {
        return this.getDockerImageName() + ":" + tagName;
    }
}
