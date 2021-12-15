package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.testcontainers.utility.DockerImageName;

/**
 * This container wraps Confluent Kafka and Zookeeper (optionally)
 *
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka");
    private static final String DEFAULT_TAG = "5.4.3";

    public static final int KAFKA_PORT = 9093;

    public static final int ZOOKEEPER_PORT = 2181;

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    protected String externalZookeeperConnect = null;

    /**
     * @deprecated use {@link KafkaContainer(DockerImageName)} instead
     */
    @Deprecated
    public KafkaContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link KafkaContainer(DockerImageName)} instead
     */
    @Deprecated
    public KafkaContainer(String confluentPlatformVersion) {
        this(DEFAULT_IMAGE_NAME.withTag(confluentPlatformVersion));
    }

    public KafkaContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(KAFKA_PORT);

        // Use two listeners with different names, it will force Kafka to communicate with itself via internal
        // listener when KAFKA_INTER_BROKER_LISTENER_NAME is set, otherwise Kafka will try to use the advertised listener
        withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
    }

    public KafkaContainer withEmbeddedZookeeper() {
        externalZookeeperConnect = null;
        return self();
    }

    public KafkaContainer withExternalZookeeper(String connectString) {
        externalZookeeperConnect = connectString;
        return self();
    }

    public String getBootstrapServers() {
        return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
    }

    @Override
    protected void configure() {
        withEnv(
            "KAFKA_ADVERTISED_LISTENERS",
            String.format(
                "BROKER://%s:9092",
                getNetwork() != null
                    ? getNetworkAliases().get(0)
                    : "localhost"
            )
        );

        String command = "#!/bin/bash\n";
        if (externalZookeeperConnect != null) {
            withEnv("KAFKA_ZOOKEEPER_CONNECT", externalZookeeperConnect);
        } else {
            addExposedPort(ZOOKEEPER_PORT);
            withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:" + ZOOKEEPER_PORT);
            command += "echo 'clientPort=" + ZOOKEEPER_PORT + "' > zookeeper.properties\n";
            command += "echo 'dataDir=/var/lib/zookeeper/data' >> zookeeper.properties\n";
            command += "echo 'dataLogDir=/var/lib/zookeeper/log' >> zookeeper.properties\n";
            command += "zookeeper-server-start zookeeper.properties &\n";
        }

        // Optimization: skip the checks
        command += "echo '' > /etc/confluent/docker/ensure \n";
        // Run the original command
        command += "/etc/confluent/docker/run \n";
        withCommand("sh", "-c", command);
    }

    @Override
    @SneakyThrows
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        String brokerAdvertisedListener = brokerAdvertisedListener(containerInfo);
        ExecResult result = execInContainer(
            "kafka-configs",
            "--alter",
            "--bootstrap-server", brokerAdvertisedListener,
            "--entity-type", "brokers",
            "--entity-name", getEnvMap().get("KAFKA_BROKER_ID"),
            "--add-config",
            "advertised.listeners=[" + String.join(",", getBootstrapServers(), brokerAdvertisedListener) + "]"
        );
        if (result.getExitCode() != 0) {
            throw new IllegalStateException(result.toString());
        }
    }

    protected String brokerAdvertisedListener(InspectContainerResponse containerInfo) {
        return String.format("BROKER://%s:%s", containerInfo.getConfig().getHostName(), "9092");
    }
}
