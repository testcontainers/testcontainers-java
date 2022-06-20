package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

/**
 * This container wraps Apache Pulsar running in standalone mode
 */
public class PulsarContainer extends GenericContainer<PulsarContainer> {

    public static final int BROKER_PORT = 6650;

    public static final int BROKER_HTTP_PORT = 8080;

    public static final String METRICS_ENDPOINT = "/metrics";

    /**
     * See <a href="https://github.com/apache/pulsar/blob/master/pulsar-common/src/main/java/org/apache/pulsar/common/naming/SystemTopicNames.java">SystemTopicNames</a>.
     */
    private static final String TRANSACTION_TOPIC_ENDPOINT =
        "/admin/v2/persistent/pulsar/system/transaction_coordinator_assign/partitions";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("apachepulsar/pulsar");

    @Deprecated
    private static final String DEFAULT_TAG = "2.10.0";

    private boolean functionsWorkerEnabled = false;

    private boolean transactionsEnabled = false;

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
    }

    @Override
    protected void configure() {
        super.configure();
        setupCommandAndEnv();
    }

    public PulsarContainer withFunctionsWorker() {
        functionsWorkerEnabled = true;
        return this;
    }

    public PulsarContainer withTransactions() {
        transactionsEnabled = true;
        return this;
    }

    public String getPulsarBrokerUrl() {
        return String.format("pulsar://%s:%s", getHost(), getMappedPort(BROKER_PORT));
    }

    public String getHttpServiceUrl() {
        return String.format("http://%s:%s", getHost(), getMappedPort(BROKER_HTTP_PORT));
    }

    protected void setupCommandAndEnv() {
        String standaloneBaseCommand =
            "/pulsar/bin/apply-config-from-env.py /pulsar/conf/standalone.conf " + "&& bin/pulsar standalone";

        if (!functionsWorkerEnabled) {
            standaloneBaseCommand += " --no-functions-worker -nss";
        }

        withCommand("/bin/bash", "-c", standaloneBaseCommand);

        List<WaitStrategy> waitStrategies = new ArrayList<>();
        waitStrategies.add(Wait.defaultWaitStrategy());
        waitStrategies.add(Wait.forHttp(METRICS_ENDPOINT).forStatusCode(200).forPort(BROKER_HTTP_PORT));
        if (transactionsEnabled) {
            withEnv("PULSAR_PREFIX_transactionCoordinatorEnabled", "true");
            waitStrategies.add(Wait.forHttp(TRANSACTION_TOPIC_ENDPOINT).forStatusCode(200).forPort(BROKER_HTTP_PORT));
        }
        if (functionsWorkerEnabled) {
            waitStrategies.add(Wait.forLogMessage(".*Function worker service started.*", 1));
        }
        final WaitAllStrategy compoundedWaitStrategy = new WaitAllStrategy();
        waitStrategies.forEach(compoundedWaitStrategy::withStrategy);
        waitingFor(compoundedWaitStrategy);
    }
}
