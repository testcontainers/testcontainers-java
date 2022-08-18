package org.testcontainers.redpanda;

import com.google.common.collect.ImmutableMap;
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
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class RedpandaContainerTest {

    private static final String REDPANDA_IMAGE = "docker.redpanda.com/vectorized/redpanda:v22.2.1";

    private static final DockerImageName REDPANDA_DOCKER_IMAGE = DockerImageName.parse(REDPANDA_IMAGE);

    @Test
    public void testUsage() throws Exception {
        try (RedpandaContainer container = new RedpandaContainer(REDPANDA_DOCKER_IMAGE)) {
            container.start();
            testKafkaFunctionality(container.getBootstrapServers());
        }
    }

    @Test
    public void testUsageWithStringImage() throws Exception {
        try (
            // constructorWithVersion {
            RedpandaContainer container = new RedpandaContainer("docker.redpanda.com/vectorized/redpanda:v22.2.1")
            // }
        ) {
            container.start();
            testKafkaFunctionality(
                // getBootstrapServers {
                container.getBootstrapServers()
                // }
            );
        }
    }

    @Test
    public void testNotCompatibleVersion() {
        assertThatThrownBy(() -> new RedpandaContainer("docker.redpanda.com/vectorized/redpanda:v21.11.19"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Redpanda version must be >= v22.2.1");
    }

    private void testKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, 1, 1);
    }

    private void testKafkaFunctionality(String bootstrapServers, int partitions, int rf) throws Exception {
        try (
            AdminClient adminClient = AdminClient.create(
                ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            );
            KafkaProducer<String, String> producer = new KafkaProducer<>(
                ImmutableMap.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    bootstrapServers,
                    ProducerConfig.CLIENT_ID_CONFIG,
                    UUID.randomUUID().toString()
                ),
                new StringSerializer(),
                new StringSerializer()
            );
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                ImmutableMap.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    bootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG,
                    "tc-" + UUID.randomUUID(),
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                    "earliest"
                ),
                new StringDeserializer(),
                new StringDeserializer()
            );
        ) {
            String topicName = "messages-" + UUID.randomUUID();

            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, partitions, (short) rf));
            adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);

            consumer.subscribe(Collections.singletonList(topicName));

            producer.send(new ProducerRecord<>(topicName, "testcontainers", "rulezzz")).get();

            Unreliables.retryUntilTrue(
                10,
                TimeUnit.SECONDS,
                () -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    if (records.isEmpty()) {
                        return false;
                    }

                    assertThat(records)
                        .hasSize(1)
                        .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
                        .containsExactly(tuple(topicName, "testcontainers", "rulezzz"));

                    return true;
                }
            );

            consumer.unsubscribe();
        }
    }
}
