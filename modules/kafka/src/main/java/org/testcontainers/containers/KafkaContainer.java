package org.testcontainers.containers;

import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

/**
 * This container wraps Confluent Kafka and Zookeeper (optionally)
 *
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {

    private static final String DEFAULT_TAG = "5.4.3";

    @Deprecated
    public static final int KAFKA_PORT = KafkaContainerDef.KAFKA_PORT;

    @Deprecated
    public static final int ZOOKEEPER_PORT = KafkaContainerDef.ZOOKEEPER_PORT;

    /**
     * @deprecated use {@link KafkaContainer(DockerImageName)} instead
     */
    @Deprecated
    public KafkaContainer() {
        this(KafkaContainerDef.DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link KafkaContainer(DockerImageName)} instead
     */
    @Deprecated
    public KafkaContainer(String confluentPlatformVersion) {
        this(KafkaContainerDef.DEFAULT_IMAGE_NAME.withTag(confluentPlatformVersion));
    }

    public KafkaContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(KafkaContainerDef.DEFAULT_IMAGE_NAME);
    }

    @Override
    BaseContainerDef<?> createContainerDef(RemoteDockerImage image) {
        return new KafkaContainerDef(image);
    }

    @Override
    KafkaContainerDef getContainerDef() {
        return (KafkaContainerDef) super.getContainerDef();
    }

    @Override
    StartedKafkaContainer getStarted() {
        return (StartedKafkaContainer) super.getStarted();
    }

    public KafkaContainer withEmbeddedZookeeper() {
        getContainerDef().withEmbeddedZookeeper();
        return self();
    }

    public KafkaContainer withExternalZookeeper(String connectString) {
        getContainerDef().setExternalZookeeper(connectString);
        return self();
    }

    public String getBootstrapServers() {
        return getStarted().getBootstrapServers();
    }
}
