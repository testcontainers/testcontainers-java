package com.example.kafkacluster;

import org.apache.kafka.common.Uuid;
import org.awaitility.Awaitility;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ApacheKafkaContainerCluster implements Startable {

    private final int brokersNum;

    private final Network network;

    private final Collection<KafkaContainer> brokers;

    public ApacheKafkaContainerCluster(String version, int brokersNum, int internalTopicsRf) {
        if (brokersNum < 0) {
            throw new IllegalArgumentException("brokersNum '" + brokersNum + "' must be greater than 0");
        }
        if (internalTopicsRf < 0 || internalTopicsRf > brokersNum) {
            throw new IllegalArgumentException(
                "internalTopicsRf '" + internalTopicsRf + "' must be less than brokersNum and greater than 0"
            );
        }

        this.brokersNum = brokersNum;
        this.network = Network.newNetwork();

        String controllerQuorumVoters = IntStream
            .range(0, brokersNum)
            .mapToObj(brokerNum -> String.format("%d@broker-%d:9094", brokerNum, brokerNum))
            .collect(Collectors.joining(","));

        String clusterId = Uuid.randomUuid().toString();

        this.brokers =
            IntStream
                .range(0, brokersNum)
                .mapToObj(brokerNum -> {
                    return new KafkaContainer(DockerImageName.parse("apache/kafka").withTag(version))
                        .withNetwork(this.network)
                        .withNetworkAliases("broker-" + brokerNum)
                        .withEnv("CLUSTER_ID", clusterId)
                        .withEnv("KAFKA_BROKER_ID", brokerNum + "")
                        .withEnv("KAFKA_NODE_ID", brokerNum + "")
                        .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", controllerQuorumVoters)
                        .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", internalTopicsRf + "")
                        .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
                        .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", internalTopicsRf + "")
                        .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", internalTopicsRf + "")
                        .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", internalTopicsRf + "")
                        .withStartupTimeout(Duration.ofMinutes(1));
                })
                .collect(Collectors.toList());
    }

    public Collection<KafkaContainer> getBrokers() {
        return this.brokers;
    }

    public String getBootstrapServers() {
        return brokers.stream().map(KafkaContainer::getBootstrapServers).collect(Collectors.joining(","));
    }

    @Override
    public void start() {
        // Needs to start all the brokers at once
        brokers.parallelStream().forEach(GenericContainer::start);

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                Container.ExecResult result =
                    this.brokers.stream()
                        .findFirst()
                        .get()
                        .execInContainer(
                            "sh",
                            "-c",
                            "/opt/kafka/bin/kafka-log-dirs.sh --bootstrap-server localhost:9093 --describe | grep -o '\"broker\"' | wc -l"
                        );
                String brokers = result.getStdout().replace("\n", "");

                assertThat(brokers).asInt().isEqualTo(this.brokersNum);
            });
    }

    @Override
    public void stop() {
        this.brokers.stream().parallel().forEach(GenericContainer::stop);
    }
}
