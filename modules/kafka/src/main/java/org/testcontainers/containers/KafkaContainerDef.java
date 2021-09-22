package org.testcontainers.containers;

import lombok.SneakyThrows;
import org.testcontainers.UnstableAPI;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

@UnstableAPI
class KafkaContainerDef extends BaseContainerDef<StartedKafkaContainer> {

    static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka");

    static final int ZOOKEEPER_PORT = 2181;

    static final int KAFKA_PORT = 9093;

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    private String externalZookeeperConnect = null;

    public static DockerImageName confluentPlatformImage(String version) {
        return DEFAULT_IMAGE_NAME.withTag(version);
    }

    public KafkaContainerDef(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public KafkaContainerDef(DockerImageName imageName) {
        this(new RemoteDockerImage(imageName));
    }

    public KafkaContainerDef(RemoteDockerImage image) {
        super(image);

        addExposedPort(KAFKA_PORT);

        // Use two listeners with different names, it will force Kafka to communicate with itself via internal
        // listener when KAFKA_INTER_BROKER_LISTENER_NAME is set, otherwise Kafka will try to use the advertised listener
        setEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        setEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
        setEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        setEnv("KAFKA_BROKER_ID", "1");
        setEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        setEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF);
        setEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        setEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF);
        setEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        setEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
    }

    @Override
    protected void configure() {
        super.configure();
        setEnv(
            "KAFKA_ADVERTISED_LISTENERS",
            String.format(
                "BROKER://%s:9092",
                getNetwork() != null
                    ? getNetworkAliases().iterator().next()
                    : "localhost"
            )
        );

        String command = "#!/bin/bash\n";
        if (externalZookeeperConnect != null) {
            setEnv("KAFKA_ZOOKEEPER_CONNECT", externalZookeeperConnect);
        } else {
            addExposedPort(ZOOKEEPER_PORT);
            setEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:" + ZOOKEEPER_PORT);
            command += "echo 'clientPort=" + ZOOKEEPER_PORT + "' > zookeeper.properties\n";
            command += "echo 'dataDir=/var/lib/zookeeper/data' >> zookeeper.properties\n";
            command += "echo 'dataLogDir=/var/lib/zookeeper/log' >> zookeeper.properties\n";
            command += "zookeeper-server-start zookeeper.properties &\n";
        }

        // Optimization: skip the checks
        command += "echo '' > /etc/confluent/docker/ensure \n";
        // Run the original command
        command += "/etc/confluent/docker/run \n";
        setCommand("sh", "-c", command);
    }

    protected void withEmbeddedZookeeper() {
        externalZookeeperConnect = null;
    }

    protected void setExternalZookeeper(String connectString) {
        externalZookeeperConnect = connectString;
    }

    @Override
    protected StartedKafkaContainer toStarted(ContainerState containerState) {
        return new Started(containerState);
    }

    class Started extends BaseContainerDef.Started implements StartedKafkaContainer, ContainerHooksAware {

        public Started(ContainerState containerState) {
            super(containerState);
        }

        @Override
        public String getBootstrapServers() {
            return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        public void beforeStart() {
            if (externalZookeeperConnect == null) {
                addExposedPort(ZOOKEEPER_PORT);
            }
        }

        @Override
        @SneakyThrows
        public void containerIsStarted(boolean reused) {
            String brokerAdvertisedListener = brokerAdvertisedListener();
            Container.ExecResult result = execInContainer(
                "kafka-configs",
                "--alter",
                "--bootstrap-server", brokerAdvertisedListener,
                "--entity-type", "brokers",
                "--entity-name", getEnv().get("KAFKA_BROKER_ID"),
                "--add-config",
                "advertised.listeners=[" + String.join(",", getBootstrapServers(), brokerAdvertisedListener) + "]"
            );
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(result.getStderr());
            }
        }

        protected String brokerAdvertisedListener() {
            return String.format("BROKER://%s:%s", getContainerInfo().getConfig().getHostName(), "9092");
        }
    }
}
