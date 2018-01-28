package org.testcontainers.containers;

import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * An ambassador container is used as a TCP proxy, enabling any TCP port of another linked container to be exposed
 * publicly, even if that container does not make the port public itself. The <code>richnorth/ambassador:latest</code>
 * container is used (based on HAProxy).
 *
 * @deprecated use {@link SocatContainer}
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Deprecated
public class AmbassadorContainer<SELF extends AmbassadorContainer<SELF>> extends GenericContainer<SELF> {

    private final String otherContainerName;
    private final String serviceName;
    private final int servicePort;

    public AmbassadorContainer(LinkableContainer otherContainer, String serviceName, int servicePort) {
        super(TestcontainersConfiguration.getInstance().getAmbassadorContainerImage());

        /*
          Use the unique 'identifierPrefix' (random compose project name) so that the ambassador can see
          the container it's supposed to be proxying.
         */
        this.otherContainerName = otherContainer.getContainerName();
        this.serviceName = serviceName;
        this.servicePort = servicePort;

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
