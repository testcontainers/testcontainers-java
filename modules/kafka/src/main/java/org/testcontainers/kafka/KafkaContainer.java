package org.testcontainers.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

/**
 * Testcontainers implementation for Apache Kafka.
 * <p>
 * Supported image: {@code apache/kafka}, {@code apache/kafka-native}
 * <p>
 * Exposed ports: 9092
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("apache/kafka");

    private static final DockerImageName APACHE_KAFKA_NATIVE_IMAGE_NAME = DockerImageName.parse("apache/kafka-native");

    private static final int KAFKA_PORT = 9092;

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    private static final String STARTER_SCRIPT = "/tmp/testcontainers_start.sh";

    private static final String DEFAULT_CLUSTER_ID = "4L6g3nShT-eMCtK--X86sw";

    public KafkaContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public KafkaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, APACHE_KAFKA_NATIVE_IMAGE_NAME);

        withExposedPorts(KAFKA_PORT);
        withEnv("CLUSTER_ID", DEFAULT_CLUSTER_ID);

        withEnv(
            "KAFKA_LISTENERS",
            "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9093, CONTROLLER://0.0.0.0:9094"
        );
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
        withEnv("KAFKA_PROCESS_ROLES", "broker,controller");
        withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER");

        withEnv("KAFKA_NODE_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF);
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

        withCommand("sh", "-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
        waitingFor(Wait.forLogMessage(".*Transitioning from RECOVERY to RUNNING.*", 1));
    }

    @Override
    protected void configure() {
        String firstNetworkAlias = getNetworkAliases().stream().findFirst().orElse(null);
        String networkAlias = getNetwork() != null ? firstNetworkAlias : "localhost";
        String controllerQuorumVoters = String.format("%s@%s:9094", getEnvMap().get("KAFKA_NODE_ID"), networkAlias);
        withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", controllerQuorumVoters);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        String brokerAdvertisedListener = String.format(
            "BROKER://%s:%s",
            containerInfo.getConfig().getHostName(),
            "9093"
        );
        List<String> advertisedListeners = new ArrayList<>();
        advertisedListeners.add("PLAINTEXT://" + getBootstrapServers());
        advertisedListeners.add(brokerAdvertisedListener);
        String kafkaAdvertisedListeners = String.join(",", advertisedListeners);
        String command = "#!/bin/bash\n";
        // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
        command += String.format("export KAFKA_ADVERTISED_LISTENERS=%s\n", kafkaAdvertisedListeners);

        command += "/etc/kafka/docker/run \n";
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }

    public String getBootstrapServers() {
        return String.format("%s:%s", getHost(), getMappedPort(KAFKA_PORT));
    }
}
