package com.example.kafkacluster;

import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides an easy way to launch a Kafka cluster with multiple brokers.
 */
public class KafkaContainerCluster implements Startable {

    private final int brokersNum;

    private final Network network;

    private final GenericContainer<?> zookeeper;

    private final Collection<KafkaContainer> brokers;

    public KafkaContainerCluster(String confluentPlatformVersion, int brokersNum, int internalTopicsRf) {
        if (brokersNum <= 0) {
            throw new IllegalArgumentException("brokersNum '" + brokersNum + "' must be greater than 0");
        }
        if (internalTopicsRf <= 0 || internalTopicsRf > brokersNum) {
            throw new IllegalArgumentException(
                "internalTopicsRf '" + internalTopicsRf + "' must be less than or equal to brokersNum and greater than 0"
            );
        }

        this.brokersNum = brokersNum;
        this.network = Network.newNetwork();

        this.zookeeper =
            new GenericContainer<>(DockerImageName.parse("confluentinc/cp-zookeeper").withTag(confluentPlatformVersion))
                .withNetwork(network)
                .withNetworkAliases("zookeeper")
                .withEnv("ZOOKEEPER_CLIENT_PORT", String.valueOf(KafkaContainer.ZOOKEEPER_PORT));

        this.brokers =
            IntStream
                .range(0, this.brokersNum)
                .mapToObj(brokerNum -> {
                    return new KafkaContainer(
                        DockerImageName.parse("confluentinc/cp-kafka").withTag(confluentPlatformVersion)
                    )
                        .withNetwork(this.network)
                        .withNetworkAliases("broker-" + brokerNum)
                        .dependsOn(this.zookeeper)
                        .withExternalZookeeper("zookeeper:" + KafkaContainer.ZOOKEEPER_PORT)
                        .withEnv("KAFKA_BROKER_ID", brokerNum + "")
                        .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", internalTopicsRf + "")
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

    private Stream<GenericContainer<?>> allContainers() {
        return Stream.concat(this.brokers.stream(), Stream.of(this.zookeeper));
    }

    @Override
    @SneakyThrows
    public void start() {
        // sequential start to avoid resource contention on CI systems with weaker hardware
        brokers.forEach(GenericContainer::start);

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                Container.ExecResult result =
                    this.zookeeper.execInContainer(
                            "sh",
                            "-c",
                            "zookeeper-shell zookeeper:" +
                            KafkaContainer.ZOOKEEPER_PORT +
                            " ls /brokers/ids | tail -n 1"
                        );
                String brokers = result.getStdout();

                assertThat(brokers.split(",")).hasSize(this.brokersNum);
            });
    }

    @Override
    public void stop() {
        allContainers().parallel().forEach(GenericContainer::stop);
    }
}
