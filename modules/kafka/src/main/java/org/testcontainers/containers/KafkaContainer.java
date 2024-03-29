package org.testcontainers.containers;

import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Testcontainers implementation for Apache Kafka.
 * Zookeeper can be optionally configured.
 * <p>
 * Supported image: {@code confluentinc/cp-kafka}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Kafka: 9093</li>
 *     <li>Zookeeper: 2181</li>
 * </ul>
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka");

    private static final String DEFAULT_TAG = "5.4.3";

    @Deprecated
    public static final int KAFKA_PORT = KafkaContainerDef.KAFKA_PORT;

    @Deprecated
    public static final int ZOOKEEPER_PORT = KafkaContainerDef.ZOOKEEPER_PORT;

    // https://docs.confluent.io/platform/7.0.0/release-notes/index.html#ak-raft-kraft
    private static final String MIN_KRAFT_TAG = "7.0.0";

    @Deprecated
    public static final String DEFAULT_CLUSTER_ID = KafkaContainerDef.DEFAULT_CLUSTER_ID;

    protected String externalZookeeperConnect = null;

    private boolean kraftEnabled = false;

    /**
     * @deprecated use {@link #KafkaContainer(DockerImageName)} instead
     */
    @Deprecated
    public KafkaContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link #KafkaContainer(DockerImageName)} instead
     */
    @Deprecated
    public KafkaContainer(String confluentPlatformVersion) {
        this(DEFAULT_IMAGE_NAME.withTag(confluentPlatformVersion));
    }

    public KafkaContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    @Override
    KafkaContainerDef createContainerDef() {
        return new KafkaContainerDef();
    }

    @Override
    KafkaContainerDef getContainerDef() {
        return (KafkaContainerDef) super.getContainerDef();
    }

    @Override
    StartedKafkaContainer getStartedContainer() {
        return (StartedKafkaContainer) super.getStartedContainer();
    }

    public KafkaContainer withEmbeddedZookeeper() {
        if (this.kraftEnabled) {
            throw new IllegalStateException("Cannot configure Zookeeper when using Kraft mode");
        }
        this.externalZookeeperConnect = null;
        return self();
    }

    public KafkaContainer withExternalZookeeper(String connectString) {
        if (this.kraftEnabled) {
            throw new IllegalStateException("Cannot configure Zookeeper when using Kraft mode");
        }
        this.externalZookeeperConnect = connectString;
        getContainerDef().withExternalZookeeperConnect(connectString);
        return self();
    }

    public KafkaContainer withKraft() {
        if (this.externalZookeeperConnect != null) {
            throw new IllegalStateException("Cannot configure Kraft mode when Zookeeper configured");
        }
        verifyMinKraftVersion();
        this.kraftEnabled = true;
        getContainerDef().withKraftEnabled();
        return self();
    }

    private void verifyMinKraftVersion() {
        String actualVersion = DockerImageName.parse(getDockerImageName()).getVersionPart();
        if (new ComparableVersion(actualVersion).isLessThan(MIN_KRAFT_TAG)) {
            throw new IllegalArgumentException(
                String.format(
                    "Provided Confluent Platform's version %s is not supported in Kraft mode (must be %s or above)",
                    actualVersion,
                    MIN_KRAFT_TAG
                )
            );
        }
    }

    public KafkaContainer withClusterId(String clusterId) {
        Objects.requireNonNull(clusterId, "clusterId cannot be null");
        getContainerDef().withClusterId(clusterId);
        return self();
    }

    public String getBootstrapServers() {
        return getStartedContainer().getBootstrapServers();
    }

    @Override
    protected void configure() {
        getContainerDef().resolveListeners();

        if (this.kraftEnabled) {
            configureKraft();
        } else {
            configureZookeeper();
        }
    }

    protected void configureKraft() {
        getContainerDef().configureKraft();
    }

    protected void configureZookeeper() {
        getContainerDef().configureZookeeper();
    }

    /**
     * Add a {@link Supplier} that will provide a listener with format {@code host:port}.
     * Host will be added as a network alias.
     * <p>
     * The listener will be added to the list of default listeners.
     * <p>
     * Default listeners:
     * <ul>
     *     <li>0.0.0.0:9092</li>
     *     <li>0.0.0.0:9093</li>
     * </ul>
     * <p>
     * Default advertised listeners:
     * <ul>
     *      <li>{@code container.getHost():container.getMappedPort(9093)}</li>
     *      <li>{@code container.getConfig().getHostName():9092}</li>
     * </ul>
     * @param listenerSupplier a supplier that will provide a listener
     * @return this {@link KafkaContainer} instance
     */
    public KafkaContainer withListener(Supplier<String> listenerSupplier) {
        getContainerDef().withListener(listenerSupplier);
        return this;
    }
}
