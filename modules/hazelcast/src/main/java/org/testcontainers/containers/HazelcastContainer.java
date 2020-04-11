package org.testcontainers.containers;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;

/**
 * See <a href="https://hub.docker.com/r/hazelcast/hazelcast/">https://hub.docker.com/r/hazelcast/hazelcast/</a>
 */
public class HazelcastContainer<SELF extends HazelcastContainer<SELF>> extends GenericContainer<SELF> {

    public static final String VERSION = "4.0.1";

    private static final String IMAGE_NAME = "hazelcast/hazelcast";
    private static final int HAZELCAST_PORT = 5701;

    public HazelcastContainer() {
        this(VERSION);
    }

    public HazelcastContainer(final String version) {
        super(IMAGE_NAME + ":" + version);
        waitStrategy = new WaitAllStrategy()
            .withStrategy(Wait.forListeningPort());
    }

    @Override
    protected void configure() {
        addExposedPort(HAZELCAST_PORT);
        addEnv("HZ_PHONE_HOME_ENABLED", "false");
    }

    /**
     * Provide a custom Hazelcast configuration file
     *
     * @param fileName a name of a valid Hazelcast configuration file
     *
     * @return a reference to this container instance
     */
    public SELF withHazelcastConfigFile(final String fileName) {
        String targetLocation = "/etc/" + fileName;
        withClasspathResourceMapping(fileName, targetLocation, BindMode.READ_ONLY);
        withHazelcastJavaOpts("-Dhazelcast.config=" + targetLocation);

        return self();
    }

    /**
     * Provide custom JAVA_OPTS for Hazelcast instance
     *
     * @param javaOpts a custom JAVA_OPTS line
     *
     * @return a reference to this container instance
     */
    public SELF withHazelcastJavaOpts(final String javaOpts) {
        getEnvMap().compute("JAVA_OPTS", (k, v) -> v == null ? javaOpts : format("%s %s", v, javaOpts));

        return self();
    }

    /**
     * Provide a custom port for Prometheus agent
     *
     * @param port a custom port number
     *
     * @return a reference to this container instance
     */
    public SELF withHazelcastPrometheusPort(final int port) {
        addExposedPort(port);
        addEnv("PROMETHEUS_PORT", String.valueOf(port));
        withHazelcastJavaOpts("-Dhazelcast.jmx=true");

        return self();
    }

    /**
     * Provide a custom JMX port
     *
     * @param port a custom port number
     *
     * @return a reference to this container instance
     */
    public SELF withHazelcastJMXPort(final int port) {
        addExposedPort(port);
        withHazelcastJavaOpts("-Dhazelcast.jmx=true -Dcom.sun.management.jmxremote.port=" + port);
        withHazelcastJavaOpts("-Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false");

        return self();
    }

    /**
     * Provide a custom Hazelcast log level
     *
     * @param level a logging level to be used by Hazelcast IMDG instance
     *
     * @return a reference to this container instance
     */
    public SELF withHazelcastLoggingLevel(final LogLevel level) {
        addEnv("LOGGING_LEVEL", level.toString());

        return self();
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(HAZELCAST_PORT));
    }

    /**
     * @return a URL to Hazelcast IMDG instance
     */
    public String getUrl() {
        return "http://" + getContainerIpAddress() + ":" + getMappedPort(HAZELCAST_PORT);
    }

    /**
     * @return a Hazelcast client
     */
    public HazelcastInstance getHazelcastClient(ClientConfig config) {
        if (!isRunning()) {
            throw new IllegalStateException("Hazelcast container must be started first!");
        }

        config.getNetworkConfig().addAddress(getUrl().replace("http://", ""));

        return HazelcastClient.newHazelcastClient(config);
    }

    /**
     * @return a Hazelcast client
     */
    public HazelcastInstance getHazelcastClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("dev");
        return getHazelcastClient(clientConfig);
    }

    enum LogLevel {
        SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST
    }
}
