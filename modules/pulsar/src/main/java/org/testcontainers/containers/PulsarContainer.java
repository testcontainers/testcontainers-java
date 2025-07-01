package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Apache Pulsar.
 * <p>
 * Supported images: {@code apachepulsar/pulsar}, {@code apachepulsar/pulsar-all}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Pulsar: 6650</li>
 *     <li>HTTP: 8080</li>
 * </ul>
 */
public class PulsarContainer extends GenericContainer<PulsarContainer> {

    public static final int BROKER_PORT = 6650;

    public static final int BROKER_HTTP_PORT = 8080;

    private static final String ADMIN_CLUSTERS_ENDPOINT = "/admin/v2/clusters";

    /**
     * See <a href="https://github.com/apache/pulsar/blob/master/pulsar-common/src/main/java/org/apache/pulsar/common/naming/SystemTopicNames.java">SystemTopicNames</a>.
     */
    private static final String TRANSACTION_TOPIC_ENDPOINT =
        "/admin/v2/persistent/pulsar/system/transaction_coordinator_assign/partitions";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("apachepulsar/pulsar");

    private final WaitAllStrategy waitAllStrategy = new WaitAllStrategy();

    private boolean functionsWorkerEnabled = false;

    private boolean transactionsEnabled = false;

    /**
     * @deprecated use {@link #PulsarContainer(DockerImageName)} instead
     */
    @Deprecated
    public PulsarContainer(String pulsarVersion) {
        this(DEFAULT_IMAGE_NAME.withTag(pulsarVersion));
    }

    public PulsarContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, DockerImageName.parse("apachepulsar/pulsar-all"));
        withExposedPorts(BROKER_PORT, BROKER_HTTP_PORT);
        setWaitStrategy(waitAllStrategy);
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

        final String clusterName = getEnvMap().getOrDefault("PULSAR_PREFIX_clusterName", "standalone");
        final String response = String.format("[\"%s\"]", clusterName);
        waitAllStrategy.withStrategy(
            Wait.forHttp(ADMIN_CLUSTERS_ENDPOINT).forPort(BROKER_HTTP_PORT).forResponsePredicate(response::equals)
        );

        if (transactionsEnabled) {
            withEnv("PULSAR_PREFIX_transactionCoordinatorEnabled", "true");
            waitAllStrategy.withStrategy(
                Wait.forHttp(TRANSACTION_TOPIC_ENDPOINT).forStatusCode(200).forPort(BROKER_HTTP_PORT)
            );
        }
        if (functionsWorkerEnabled) {
            waitAllStrategy.withStrategy(Wait.forLogMessage(".*Function worker service started.*", 1));
        }
    }
}
