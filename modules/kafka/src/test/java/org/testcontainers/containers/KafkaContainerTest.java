package org.testcontainers.containers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.ClassRule;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.utility.DockerImageName;

public class KafkaContainerTest {

    private static final DockerImageName KAFKA_TEST_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:5.4.3");
    private static final DockerImageName ZOOKEEPER_TEST_IMAGE = DockerImageName.parse("confluentinc/cp-zookeeper:4.0.0");

    // junitRule {
    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));
    // }

    @Test
    public void testUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(KAFKA_TEST_IMAGE)) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }


    @Test
    public void testUsageWithSpecificImage() throws Exception {
        try (
            // constructorWithVersion {
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
            // }
        ) {
            kafka.start();
            testKafkaFunctionality(
              // getBootstrapServers {
              kafka.getBootstrapServers()
              // }
            );
        }
    }


    @Test
    public void testUsageWithVersion() throws Exception {
        try (
            KafkaContainer kafka = new KafkaContainer("5.5.1")
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testExternalZookeeperWithExternalNetwork() throws Exception {
        try (
            Network network = Network.newNetwork();

            // withExternalZookeeper {
            KafkaContainer kafka = new KafkaContainer(KAFKA_TEST_IMAGE)
                .withNetwork(network)
                .withExternalZookeeper("zookeeper:2181");
            // }

            GenericContainer<?> zookeeper = new GenericContainer<>(ZOOKEEPER_TEST_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("zookeeper")
                .withEnv("ZOOKEEPER_CLIENT_PORT", "2181");

            // withKafkaNetwork {
            GenericContainer<?> application = new GenericContainer<>(DockerImageName.parse("alpine"))
                .withNetwork(network)
            // }
                .withNetworkAliases("dummy")
                .withCommand("sleep 10000")
        ) {
            zookeeper.start();
            kafka.start();
            application.start();

            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testConfluentPlatformVersion6() throws Exception {
        try (
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.0.0"))
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    protected void testKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, 1, 1);
    }

    protected void testKafkaFunctionality(String bootstrapServers, int partitions, int rf) throws Exception {
        try (
            AdminClient adminClient = AdminClient.create(ImmutableMap.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
            ));

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
            String topicName = "messages-" + UUID.randomUUID();

            Collection<NewTopic> topics = singletonList(new NewTopic(topicName, partitions, (short) rf));
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

            consumer.subscribe(singletonList(topicName));

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

}
