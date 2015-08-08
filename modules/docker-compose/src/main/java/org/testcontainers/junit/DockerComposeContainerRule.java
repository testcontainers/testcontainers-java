package org.testcontainers.junit;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerRule extends GenericContainerRule {

    private final File composeFile;
    private Map<String, GenericContainer> ambassadorContainers = new HashMap<>();

    public DockerComposeContainerRule(File composeFile) {
        super(new DockerComposeContainer(composeFile, "up"));
        this.composeFile = composeFile;
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        for (GenericContainer ambassadorContainer : ambassadorContainers.values()) {
            ambassadorContainer.start();
        }
    }

    @Override
    protected void after() {
        super.after();

        for (GenericContainer ambassadorContainer : ambassadorContainers.values()) {
            ambassadorContainer.stop();
        }
//        new DockerComposeContainer(composeFile, "kill").start();
//        new DockerComposeContainer(composeFile, "rm").start();
    }

    @Override
    public DockerComposeContainerRule withExposedPorts(int... ports) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DockerComposeContainerRule withExposedPorts(String... ports) {
         throw new UnsupportedOperationException();
    }

    public DockerComposeContainerRule withExposedService(String serviceName, String servicePort) {

        String identifier = ((DockerComposeContainer) container).getIdentifier();

        GenericContainer ambassadorContainer = new GenericContainer("richnorth/ambassador:latest");
        String otherContainerName = identifier + "_" + serviceName;

        ambassadorContainer.addLink(otherContainerName, otherContainerName);
        ambassadorContainer.addExposedPort(servicePort);
        ambassadorContainer.addEnv("SERVICE_NAME", otherContainerName);
        ambassadorContainer.addEnv("SERVICE_PORT", servicePort);

        ambassadorContainers.put(serviceName + ":" + servicePort, ambassadorContainer);

        return this;
    }

    public String getServiceHost(String serviceName, String servicePort) {
        return ambassadorContainers.get(serviceName + ":" + servicePort).getIpAddress();
    }

    public String getServicePort(String serviceName, String servicePort) {
        return ambassadorContainers.get(serviceName + ":" + servicePort).getMappedPort(servicePort);
    }
}
