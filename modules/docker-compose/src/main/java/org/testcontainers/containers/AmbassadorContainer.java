package org.testcontainers.containers;

/**
 * Created by rnorth on 10/08/2015.
 */
public class AmbassadorContainer extends GenericContainer {

    public AmbassadorContainer(DockerComposeContainer otherContainer, String serviceName, String servicePort) {
        super("richnorth/ambassador:latest");

        /**
         * Use the unique 'identifier' (random compose project name) so that the ambassador can see
         * the container it's supposed to be proxying.
         */
        String identifier = otherContainer.getIdentifier();
        String otherContainerName = identifier + "_" + serviceName;



        // Link
        addLink(otherContainerName, otherContainer.getIdentifier());

        // Expose ambassador's port
        addExposedPort(servicePort);

        // Tell the proxy what to connect to within the docker network
        addEnv("SERVICE_NAME", otherContainerName);
        addEnv("SERVICE_PORT", servicePort);
    }
}
