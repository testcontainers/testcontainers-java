package org.testcontainers.containers;

import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.model.Container;
import org.testcontainers.containers.traits.LinkableContainer;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by rnorth on 10/08/2015.
 */
public class AmbassadorContainer extends GenericContainer {

    private final String otherContainerName;

    public AmbassadorContainer(LinkableContainer otherContainer, String serviceName, int servicePort) {
        super("richnorth/ambassador:latest");

        /**
         * Use the unique 'identifierPrefix' (random compose project name) so that the ambassador can see
         * the container it's supposed to be proxying.
         */
        otherContainerName = otherContainer.getContainerName();

        // Link
        addLink(otherContainer, serviceName);

        // Expose ambassador's port
        addExposedPort(servicePort);

        // Tell the proxy what to connect to within the docker network
        addEnv("SERVICE_NAME", serviceName);
        addEnv("SERVICE_PORT", String.format("%d", servicePort));
    }

    public boolean isServiceReady() {
        try {
            List<Container> allContainers = dockerClient.listContainersCmd().withShowAll(true).exec();

            for (Container container : allContainers) {
                if (asList(container.getNames()).contains("/" + otherContainerName) && !container.getStatus().contains("Exited")) {
                    return true;
                }
            }

        } catch (DockerException e) {
            return false;
        }
        return false;
    }
}
