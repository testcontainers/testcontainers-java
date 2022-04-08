package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * This container wraps Apache Pulsar running in standalone mode
 */
public class PulsarContainer extends GenericContainer<PulsarContainer> {

    public static final int BROKER_PORT = 6650;
    public static final int BROKER_HTTP_PORT = 8080;
    public static final String METRICS_ENDPOINT = "/metrics";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("apachepulsar/pulsar");
    @Deprecated
    private static final String DEFAULT_TAG = "2.2.0";

    private boolean functionsWorkerEnabled = false;

    private Duration startupTimeout;

    /**
     * @deprecated use {@link PulsarContainer(DockerImageName)} instead
     */
    @Deprecated
    public PulsarContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link PulsarContainer(DockerImageName)} instead
     */
    @Deprecated
    public PulsarContainer(String pulsarVersion) {
        this(DEFAULT_IMAGE_NAME.withTag(pulsarVersion));
    }

    public PulsarContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DockerImageName.parse("apachepulsar/pulsar"));

        withExposedPorts(BROKER_PORT, BROKER_HTTP_PORT);
        withCommand("/pulsar/bin/pulsar", "standalone", "--no-functions-worker", "-nss");
        waitingFor(Wait.forHttp(METRICS_ENDPOINT).forStatusCode(200).forPort(BROKER_HTTP_PORT));
    }

    @Override
    protected void configure() {
        super.configure();

        if (functionsWorkerEnabled) {
            withCommand("/pulsar/bin/pulsar", "standalone");
            waitingFor(
                new WaitAllStrategy()
                    .withStrategy(waitStrategy)
                    .withStrategy(createLogWaitingStrategy())
            );
        }
    }

    @Override
    public PulsarContainer withStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
        return super.withStartupTimeout(startupTimeout);
    }

    public PulsarContainer withFunctionsWorker() {
        functionsWorkerEnabled = true;
        return this;
    }

    private WaitStrategy createLogWaitingStrategy() {
        if (startupTimeout != null) {
            return Wait.forLogMessage(".*Function worker service started.*", 1).withStartupTimeout(startupTimeout);
        }
        return Wait.forLogMessage(".*Function worker service started.*", 1);
    }

    public String getPulsarBrokerUrl() {
        return String.format("pulsar://%s:%s", getHost(), getMappedPort(BROKER_PORT));
    }

    public String getHttpServiceUrl() {
        return String.format("http://%s:%s", getHost(), getMappedPort(BROKER_HTTP_PORT));
    }
}
