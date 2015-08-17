package org.testcontainers.junit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.testcontainers.containers.AmbassadorContainer;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.Retryables;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerRule extends GenericContainerRule {

    private Map<String, AmbassadorContainer> ambassadorContainers = new HashMap<>();

    public static final Logger LOGGER = LoggerFactory.getLogger(DockerComposeContainerRule.class);

    public DockerComposeContainerRule(File composeFile) {
        super(new DockerComposeContainer(composeFile, "up -d"));
    }

    @Override
    protected void before() throws Throwable {

        Profiler profiler = new Profiler("Docker compose container rule");
        profiler.setLogger(LOGGER);
        profiler.start("Docker compose container startup");
        try {

            super.before();

            // Start any ambassador containers we need
            profiler.start("Ambassador container startup");
            for (final AmbassadorContainer ambassadorContainer : ambassadorContainers.values()) {

                Profiler localProfiler = profiler.startNested("Ambassador container: " + ambassadorContainer.getContainerName());

                /**
                 * Because docker compose might not have launched the service yet we have to wait until it is ready
                 */
                localProfiler.start("Wait for service to be ready");
                Retryables.retryUntilTrue(60, TimeUnit.SECONDS, new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return ambassadorContainer.isServiceReady();
                    }
                });

                localProfiler.start("Start ambassador container");
                ambassadorContainer.start();
            }

            // Make sure all the ambassador containers are started and proxying
            profiler.start("Wait for all ambassador containers to be started and proxying");
            for (final Map.Entry<String, AmbassadorContainer> address : ambassadorContainers.entrySet()) {

                Retryables.retryUntilSuccess(60, TimeUnit.SECONDS, new Retryables.UnreliableSupplier<Object>() {
                    @Override
                    public Object get() throws Exception {

                        GenericContainer ambassadorContainer = address.getValue();
                        String originalPort = address.getKey().split(":")[1];

                        String ipAddress = ambassadorContainer.getIpAddress();
                        Integer port = Integer.valueOf(ambassadorContainer.getMappedPort(originalPort));
                        try {
                            Socket socket = new Socket(ipAddress, port);
                            socket.close();
                        } catch (IOException e) {
                            throw new IOException("Test connection to container (via ambassador container) could not be established (" + ipAddress + ":" + port + ")", e);
                        }
                        return null;
                    }
                });
            }
        } finally {
            profiler.stop().log();
        }
    }

    @Override
    protected void after() {
        super.after();

        // Kill all the ambassador containers
        for (GenericContainer ambassadorContainer : ambassadorContainers.values()) {
            ambassadorContainer.stop();
        }
    }

    @Override
    public DockerComposeContainerRule withExposedPorts(int... ports) {
        throw new UnsupportedOperationException("Use withExposedService instead");
    }

    @Override
    public DockerComposeContainerRule withExposedPorts(String... ports) {
         throw new UnsupportedOperationException("Use withExposedService instead");
    }

    public DockerComposeContainerRule withExposedService(String serviceName, String servicePort) {

        /**
         * For every service/port pair that needs to be exposed, we have to start an 'ambassador container'.
         *
         * The ambassador container's role is to link (within the Docker network) to one of the
         * compose services, and proxy TCP network I/O out to a port that the ambassador container
         * exposes.
         *
         * This avoids the need for the docker compose file to explicitly expose ports on all the
         * services.
         */
        AmbassadorContainer ambassadorContainer = new AmbassadorContainer((DockerComposeContainer) container, serviceName, servicePort);

        // Ambassador containers will all be started together after docker compose has started
        ambassadorContainers.put(serviceName + ":" + servicePort, ambassadorContainer);

        return this;
    }

    /**
     * Get the host (e.g. IP address or hostname) that the service can be found at, from the host machine
     * (i.e. should be the machine that's running this Java process)
     * @param serviceName
     * @param servicePort
     * @return
     */
    public String getServiceHost(String serviceName, String servicePort) {
        return ambassadorContainers.get(serviceName + ":" + servicePort).getIpAddress();
    }

    public String getServicePort(String serviceName, String servicePort) {
        return ambassadorContainers.get(serviceName + ":" + servicePort).getMappedPort(servicePort);
    }
}
