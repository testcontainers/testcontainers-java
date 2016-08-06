package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.testcontainers.dockerclient.*;

import java.util.List;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Singleton class that provides initialized Docker clients.
 * <p>
 * The correct client configuration to use will be determined on first use, and cached thereafter.
 */
public class DockerClientFactory {

    private static DockerClientFactory instance;
    private static final Logger LOGGER = getLogger(DockerClientFactory.class);

    // Cached client configuration
    private DockerClientProviderStrategy strategy;
    private boolean preconditionsChecked = false;

    private static final List<DockerClientProviderStrategy> CONFIGURATION_STRATEGIES =
            asList(new EnvironmentAndSystemPropertyClientProviderStrategy(),
                    new ProxiedUnixSocketClientProviderStrategy(),
                    new UnixSocketClientProviderStrategy(),
                    new DockerMachineClientProviderStrategy());
    private String activeApiVersion;
    private String activeExecutionDriver;

    static {
        System.setProperty("org.testcontainers.shaded.io.netty.packagePrefix", "org.testcontainers.shaded.");
    }

    /**
     * Private constructor
     */
    private DockerClientFactory() {

    }

    /**
     * Obtain an instance of the DockerClientFactory.
     *
     * @return the singleton instance of DockerClientFactory
     */
    public synchronized static DockerClientFactory instance() {
        if (instance == null) {
            instance = new DockerClientFactory();
        }

        return instance;
    }

    /**
     *
     * @return a new initialized Docker client
     */
    @Synchronized
    public DockerClient client() {

        if (strategy != null) {
            return strategy.getClient();
        }

        strategy = DockerClientProviderStrategy.getFirstValidStrategy(CONFIGURATION_STRATEGIES);
        DockerClient client = strategy.getClient();

        if (!preconditionsChecked) {
            Info dockerInfo = client.infoCmd().exec();
            Version version = client.versionCmd().exec();
            activeApiVersion = version.getApiVersion();
            activeExecutionDriver = dockerInfo.getExecutionDriver();
            LOGGER.info("Connected to docker: \n" +
                    "  Server Version: " + dockerInfo.getServerVersion() + "\n" +
                    "  API Version: " + activeApiVersion + "\n" +
                    "  Operating System: " + dockerInfo.getOperatingSystem() + "\n" +
                    "  Total Memory: " + dockerInfo.getMemTotal() / (1024 * 1024) + " MB");

            checkVersion(version.getVersion());
            checkDiskSpaceAndHandleExceptions(client);
            preconditionsChecked = true;
        }

        return client;
    }

    /**
     * @return the IP address of the host running Docker
     */
    public String dockerHostIpAddress() {
        return strategy.getDockerHostIpAddress();
    }

    private void checkVersion(String version) {
        String[] splitVersion = version.split("\\.");
        if (Integer.valueOf(splitVersion[0]) <= 1 && Integer.valueOf(splitVersion[1]) < 6) {
            throw new IllegalStateException("Docker version 1.6.0+ is required, but version " + version + " was found");
        }
    }

    private void checkDiskSpaceAndHandleExceptions(DockerClient client) {
        try {
            checkDiskSpace(client);
        } catch (NotEnoughDiskSpaceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Encountered and ignored error while checking disk space", e);
        }
    }

    /**
     * Check whether this docker installation is likely to have disk space problems
     * @param client an active Docker client
     */
    private void checkDiskSpace(DockerClient client) {

        List<Image> images = client.listImagesCmd().exec();
        if (!images.stream().anyMatch(it -> asList(it.getRepoTags()).contains("alpine:3.2"))) {
            PullImageResultCallback callback = client.pullImageCmd("alpine:3.2").exec(new PullImageResultCallback());
            callback.awaitSuccess();
        }

        CreateContainerResponse createContainerResponse = client.createContainerCmd("alpine:3.2").withCmd("df", "-P").exec();
        String id = createContainerResponse.getId();

        client.startContainerCmd(id).exec();

        LogContainerResultCallback callback = client.logContainerCmd(id).withStdOut(true).exec(new LogContainerCallback());
        try {

            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
            client.waitContainerCmd(id).exec(waitCallback);
            waitCallback.awaitStarted();

            callback.awaitCompletion();
            String logResults = callback.toString();

            int availableKB = 0;
            int use = 0;
            String[] lines = logResults.split("\n");
            for (String line : lines) {
                String[] fields = line.split("\\s+");
                if (fields[5].equals("/")) {
                    availableKB = Integer.valueOf(fields[3]);
                    use = Integer.valueOf(fields[4].replace("%", ""));
                }
            }
            int availableMB = availableKB / 1024;

            LOGGER.info("Disk utilization in Docker environment is {}% ({} MB available)", use, availableMB);

            if (availableMB < 2048) {
                LOGGER.error("Docker environment has less than 2GB free - execution is unlikely to succeed so will be aborted.");
                throw new NotEnoughDiskSpaceException("Not enough disk space in Docker environment");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec();
            } catch (NotFoundException | InternalServerErrorException ignored) {

            }
        }
    }

    /**
     * @return the docker API version of the daemon that we have connected to
     */
    public String getActiveApiVersion() {
        if (!preconditionsChecked) {
            client();
        }
        return activeApiVersion;
    }

    /**
     * @return the docker execution driver of the daemon that we have connected to
     */
    public String getActiveExecutionDriver() {
        if (!preconditionsChecked) {
            client();
        }
        return activeExecutionDriver;
    }

    /**
     * @param providerStrategyClass a class that extends {@link DockerMachineClientProviderStrategy}
     * @return whether or not the currently active strategy is of the provided type
     */
    public boolean isUsing(Class<? extends DockerClientProviderStrategy> providerStrategyClass) {
        return providerStrategyClass.isAssignableFrom(this.strategy.getClass());
    }

    private static class NotEnoughDiskSpaceException extends RuntimeException {
        NotEnoughDiskSpaceException(String message) {
            super(message);
        }
    }
}

class LogContainerCallback extends LogContainerResultCallback {
    private final StringBuffer log = new StringBuffer();

    @Override
    public void onNext(Frame frame) {
        log.append(new String(frame.getPayload()));
        super.onNext(frame);
    }

    @Override
    public String toString() {
        return log.toString();
    }
}