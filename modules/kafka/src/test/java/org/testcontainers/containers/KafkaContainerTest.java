package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class KafkaContainerTest {

    @Test
    public void testUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer()) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testTransactionalUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer()) {
            kafka.start();
            testKafkaTransactionalFunctionality(kafka.getBootstrapServers());
        }
    }

    /**
     * @deprecated the {@link Network} should be set explicitly with {@link KafkaContainer#withNetwork(Network)}.
     */
    @Test
    @Deprecated
    public void testExternalZookeeperWithKafkaNetwork() throws Exception {
        try (
            KafkaContainer kafka = new KafkaContainer()
                .withExternalZookeeper("zookeeper:2181");

            GenericContainer zookeeper = new GenericContainer("confluentinc/cp-zookeeper:4.0.0")
                .withNetwork(kafka.getNetwork())
                .withNetworkAliases("zookeeper")
                .withEnv("ZOOKEEPER_CLIENT_PORT", "2181");
        ) {
            zookeeper.start();
            kafka.start();

            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testExternalZookeeperWithExternalNetwork() throws Exception {
        try (
            Network network = Network.newNetwork();

            KafkaContainer kafka = new KafkaContainer()
                .withNetwork(network)
                .withExternalZookeeper("zookeeper:2181");

            GenericContainer zookeeper = new GenericContainer("confluentinc/cp-zookeeper:4.0.0")
                .withNetwork(network)
                .withNetworkAliases("zookeeper")
                .withEnv("ZOOKEEPER_CLIENT_PORT", "2181");
        ) {
            zookeeper.start();
            kafka.start();

            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testTransactionalExternalZookeeperWithExternalNetwork() throws Exception {
        try (
            Network network = Network.newNetwork();

            KafkaContainer kafka = new KafkaContainer()
                .withNetwork(network)
                .withExternalZookeeper("zookeeper:2181");

            GenericContainer zookeeper = new GenericContainer("confluentinc/cp-zookeeper:4.0.0")
                .withNetwork(network)
                .withNetworkAliases("zookeeper")
                .withEnv("ZOOKEEPER_CLIENT_PORT", "2181");
        ) {
            zookeeper.start();
            kafka.start();

            testKafkaTransactionalFunctionality(kafka.getBootstrapServers());
        }
    }

    protected void testKafkaFunctionality(String bootstrapServers) throws Exception {
        try (
            KafkaProducer<String, String> producer = new KafkaProducer<>(
                ImmutableMap.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString()
                ),
                new StringSerializer(),
                new StringSerializer()
            );

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                ImmutableMap.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID(),
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                new StringDeserializer()
            );
        ) {
            String topicName = "messages";
            consumer.subscribe(Arrays.asList(topicName));

            producer.send(new ProducerRecord<>(topicName, "testcontainers", "rulezzz")).get();

            Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                if (records.isEmpty()) {
                    return false;
                }

                assertThat(records)
                    .hasSize(1)
                    .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
                    .containsExactly(tuple(topicName, "testcontainers", "rulezzz"));

                return true;
            });

            consumer.unsubscribe();
        }
    }

    private KafkaProducer<String, String> makeTransactionalProducer(Map<String, Object> producerProperties) {
        KafkaProducer<String, String> producer = new KafkaProducer<>(
            Collections.unmodifiableMap(producerProperties),
            new StringSerializer(),
            new StringSerializer()
        );
        producer.initTransactions();
        return producer;
    }

    protected void testKafkaTransactionalFunctionality(String bootstrapServers) throws Exception {
        Map<String, Object> producerProperties = new HashMap<>();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        producerProperties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-0");
        producerProperties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerProperties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 300000);
        producerProperties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);

        try (
            KafkaProducer<String, String> producer = makeTransactionalProducer(
                Collections.unmodifiableMap(producerProperties)
            );

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                ImmutableMap.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID(),
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",

                    ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed",
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false
        ),
                new StringDeserializer(),
                new StringDeserializer()
            );
        ) {
            String topicName = "transactional-messages";
            consumer.subscribe(Arrays.asList(topicName));

            producer.beginTransaction();
            producer.send(new ProducerRecord<>(topicName, "testcontainers", "rulezzz")).get();
            producer.commitTransaction();

            Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                if (records.isEmpty()) {
                    return false;
                }

                assertThat(records)
                    .hasSize(1)
                    .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
                    .containsExactly(tuple(topicName, "testcontainers", "rulezzz"));

                return true;
            });

            consumer.unsubscribe();
        }
    }

}
