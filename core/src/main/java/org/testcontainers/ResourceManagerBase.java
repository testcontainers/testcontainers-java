package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Network;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

abstract class ResourceManagerBase implements ResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerBase.class);

    private Map<String, String> registeredContainers = new ConcurrentHashMap<>();
    private Set<String> registeredNetworks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private AtomicBoolean hookIsSet = new AtomicBoolean(false);
    protected final DockerClient dockerClient;

    ResourceManagerBase(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public synchronized void performCleanup() {
        registeredContainers.forEach(this::stopContainer);
        registeredNetworks.forEach(this::removeNetwork);
    }

    public void registerContainerForCleanup(String containerId, String imageName) {
        setHook();
        registeredContainers.put(containerId, imageName);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     */
    public void stopAndRemoveContainer(String containerId) {
        stopContainer(containerId, registeredContainers.get(containerId));

        registeredContainers.remove(containerId);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    public void stopAndRemoveContainer(String containerId, String imageName) {
        stopContainer(containerId, imageName);

        registeredContainers.remove(containerId);
    }


    /**
     * Register a network to be cleaned up at JVM shutdown.
     *
     * @param id   the ID of the network
     */
    public void registerNetworkIdForCleanup(String id) {
        setHook();
        registeredNetworks.add(id);
    }

    public void removeNetworkById(String id) {
        removeNetwork(id);
    }

    public void unregisterNetwork(String identifier) {
        registeredNetworks.remove(identifier);
    }

    public void unregisterContainer(String identifier) {
        registeredContainers.remove(identifier);
    }

    private void setHook() {
        if (hookIsSet.compareAndSet(false, true)) {
            // If the JVM stops without containers being stopped, try and stop the container.
            Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, this::performCleanup));
        }
    }

    private void stopContainer(String containerId, String imageName) {
        boolean running;
        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            running = containerInfo.getState().getRunning();
        } catch (NotFoundException e) {
            LOGGER.trace("Was going to stop container but it apparently no longer exists: {}");
            return;
        } catch (DockerException e) {
            LOGGER.trace("Error encountered when checking container for shutdown (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
            return;
        }

        if (running) {
            try {
                LOGGER.trace("Stopping container: {}", containerId);
                dockerClient.killContainerCmd(containerId).exec();
                LOGGER.trace("Stopped container: {}", imageName);
            } catch (DockerException e) {
                LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
            }
        }

        try {
            dockerClient.inspectContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            LOGGER.trace("Was going to remove container but it apparently no longer exists: {}");
            return;
        }

        try {
            LOGGER.trace("Removing container: {}", containerId);
            try {
                dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
                LOGGER.debug("Removed container and associated volume(s): {}", imageName);
            } catch (InternalServerErrorException e) {
                LOGGER.trace("Exception when removing container with associated volume(s): {} (due to {})", imageName, e.getMessage());
            }
        } catch (DockerException e) {
            LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
        }
    }

    private void removeNetwork(String id) {
        try {
            List<Network> networks;
            try {
                // Try to find the network if it still exists
                // Listing by ID first prevents docker-java logging an error if we just go blindly into removeNetworkCmd
                networks = dockerClient.listNetworksCmd().withIdFilter(id).exec();
            } catch (Exception e) {
                LOGGER.trace("Error encountered when looking up network for removal (name: {}) - it may not have been removed", id);
                return;
            }

            // at this point networks should contain either 0 or 1 entries, depending on whether the network exists
            // using a for loop we essentially treat the network like an optional, only applying the removal if it exists
            for (Network network : networks) {
                try {
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                    registeredNetworks.remove(network.getId());
                    LOGGER.debug("Removed network: {}", id);
                } catch (Exception e) {
                    LOGGER.trace("Error encountered removing network (name: {}) - it may not have been removed", network.getName());
                }
            }
        } finally {
            registeredNetworks.remove(id);
        }
    }

    static class FilterRegistry {

        @VisibleForTesting
        static final String ACKNOWLEDGMENT = "ACK";

        private final BufferedReader in;
        private final OutputStream out;

        FilterRegistry(InputStream ryukInputStream, OutputStream ryukOutputStream) {
            this.in = new BufferedReader(new InputStreamReader(ryukInputStream));
            this.out = ryukOutputStream;
        }

        /**
         * Registers the given filters with Ryuk
         *
         * @param filters the filter to register
         * @return true if the filters have been registered successfuly, false otherwise
         * @throws IOException if communication with Ryuk fails
         */
        boolean register(List<Map.Entry<String, String>> filters) throws IOException {
            String query = URLEncodedUtils.format(
                filters.stream()
                    .map(it -> new BasicNameValuePair(it.getKey(), it.getValue()))
                    .collect(Collectors.toList()),
                (String) null
            );

            LOGGER.debug("Sending: {}", query);
            out.write(query.getBytes());
            out.write('\n');
            out.flush();

            return waitForAcknowledgment(in);
        }

        private static boolean waitForAcknowledgment(BufferedReader in) throws IOException {
            String line = in.readLine();
            while (line != null && !ACKNOWLEDGMENT.equalsIgnoreCase(line)) {
                line = in.readLine();
                if (line != null && line.length() > 0) {
                    LOGGER.debug("Received: {}", line);
                }
            }
            return ACKNOWLEDGMENT.equalsIgnoreCase(line);
        }
    }
}
