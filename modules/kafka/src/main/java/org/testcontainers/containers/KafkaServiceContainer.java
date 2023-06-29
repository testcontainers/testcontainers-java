package org.testcontainers.containers;

import lombok.AccessLevel;
import lombok.Getter;
import org.testcontainers.core.ContainerDef;
import org.testcontainers.utility.DockerImageName;

@Getter(AccessLevel.MODULE)
public class KafkaServiceContainer {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka");

    private static final int KAFKA_PORT = 9093;

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private boolean embeddedZookeeper = true;

    private String externalZookeeperConnect = null;

    private ContainerDef containerDef;

    private boolean raftMode = false;

    public void withEmbeddedZookeeper(boolean embeddedZookeeper) {
        this.embeddedZookeeper = embeddedZookeeper;
    }

    public void withExternalZookeeperConnect(String externalZookeeperConnect) {
        this.externalZookeeperConnect = externalZookeeperConnect;
    }

    public void withRaftMode(boolean raftMode) {
        this.raftMode = raftMode;
    }

    static KafkaServiceContainer from(String image) {
        return from(DockerImageName.parse(image));
    }

    static KafkaServiceContainer from(DockerImageName image) {
        KafkaServiceContainer def = new KafkaServiceContainer();
        image.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        def.containerDef =
            ContainerDef
                .from(image)
                .withExposedPorts(KAFKA_PORT)
                .withEnvVar("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092")
                .withEnvVar("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                .withEnvVar("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                .withEnvVar("KAFKA_BROKER_ID", "1")
                .withEnvVar("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF)
                .withEnvVar("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF)
                .withEnvVar("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF)
                .withEnvVar("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF)
                .withEnvVar("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "")
                .withEnvVar("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                .withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
        return def;
    }
}
