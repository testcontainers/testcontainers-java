package org.testcontainers;

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
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.rnorth.ducttape.unreliables.Unreliables;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AbstractKafka {

    private final ImmutableMap<String, String> properties = ImmutableMap.of(
        AdminClientConfig.SECURITY_PROTOCOL_CONFIG,
        "SASL_PLAINTEXT",
        SaslConfigs.SASL_MECHANISM,
        "PLAIN",
        SaslConfigs.SASL_JAAS_CONFIG,
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin\";"
    );

    protected void testKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, false, 1, 1);
    }

    protected void testSecureKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, true, 1, 1);
    }

    protected void testKafkaFunctionality(String bootstrapServers, boolean authenticated, int partitions, int rf)
        throws Exception {
        ImmutableMap<String, String> adminClientDefaultProperties = ImmutableMap.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers
        );
        Properties adminClientProperties = new Properties();
        adminClientProperties.putAll(adminClientDefaultProperties);

        ImmutableMap<String, String> consumerDefaultProperties = ImmutableMap.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,
            "tc-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest"
        );
        Properties consumerProperties = new Properties();
        consumerProperties.putAll(consumerDefaultProperties);

        ImmutableMap<String, String> producerDefaultProperties = ImmutableMap.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ProducerConfig.CLIENT_ID_CONFIG,
            UUID.randomUUID().toString()
        );
        Properties producerProperties = new Properties();
        producerProperties.putAll(producerDefaultProperties);

        if (authenticated) {
            adminClientProperties.putAll(this.properties);
            consumerProperties.putAll(this.properties);
            producerProperties.putAll(this.properties);
        }
        try (
            AdminClient adminClient = AdminClient.create(adminClientProperties);
            KafkaProducer<String, String> producer = new KafkaProducer<>(
                producerProperties,
                new StringSerializer(),
                new StringSerializer()
            );
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                consumerProperties,
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
