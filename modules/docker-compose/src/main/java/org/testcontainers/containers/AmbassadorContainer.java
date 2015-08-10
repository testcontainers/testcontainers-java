package org.testcontainers.containers;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;

import java.util.List;

/**
 * Created by rnorth on 10/08/2015.
 */
public class AmbassadorContainer extends GenericContainer {

    private final String otherContainerName;
    private final String identifierPrefix;

    public AmbassadorContainer(DockerComposeContainer otherContainer, String serviceName, String servicePort) {
        super("richnorth/ambassador:latest");

        /**
         * Use the unique 'identifierPrefix' (random compose project name) so that the ambassador can see
         * the container it's supposed to be proxying.
         */
        identifierPrefix = otherContainer.getIdentifierPrefix();
        otherContainerName = identifierPrefix + "_" + serviceName;

        // Link
        addLink(otherContainerName, otherContainer.getIdentifierPrefix());

        // Expose ambassador's port
        addExposedPort(servicePort);

        // Tell the proxy what to connect to within the docker network
        addEnv("SERVICE_NAME", otherContainerName);
        addEnv("SERVICE_PORT", servicePort);
    }

    public boolean isServiceReady() {
        try {
            List<Container> allContainers = dockerClient.listContainers(DockerClient.ListContainersParam.allContainers(true));

            for (Container container : allContainers) {
                if (container.names().contains("/" + otherContainerName) && !container.status().contains("Exited")) {
                    return true;
                }
            }

        } catch (DockerException | InterruptedException e) {
            return false;
        }
        return false;
    }
}
