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
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AbstractKafka {

    private static final ImmutableMap<String, String> PLAIN_PROPERTIES = ImmutableMap.of(
        AdminClientConfig.SECURITY_PROTOCOL_CONFIG,
        "SASL_PLAINTEXT",
        SaslConfigs.SASL_MECHANISM,
        "PLAIN",
        SaslConfigs.SASL_JAAS_CONFIG,
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin\";"
    );

    private static final ImmutableMap<String, String> SCRAM_PROPERTIES = ImmutableMap.of(
        AdminClientConfig.SECURITY_PROTOCOL_CONFIG,
        "SASL_PLAINTEXT",
        SaslConfigs.SASL_MECHANISM,
        "SCRAM-SHA-256",
        SaslConfigs.SASL_JAAS_CONFIG,
        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"admin\" password=\"admin\";"
    );

    protected void testKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, false, 1, 1);
    }

    protected void testSecurePlainKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, true, PLAIN_PROPERTIES, 1, 1);
    }

    protected void testSecureScramKafkaFunctionality(String bootstrapServers) throws Exception {
        testKafkaFunctionality(bootstrapServers, true, SCRAM_PROPERTIES, 1, 1);
    }

    protected void testKafkaFunctionality(String bootstrapServers, boolean authenticated, int partitions, int rf)
        throws Exception {
        testKafkaFunctionality(bootstrapServers, authenticated, Collections.emptyMap(), partitions, rf);
    }

    protected void testKafkaFunctionality(
        String bootstrapServers,
        boolean authenticated,
        Map<String, String> authProperties,
        int partitions,
        int rf
    ) throws Exception {
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
            adminClientProperties.putAll(authProperties);
            consumerProperties.putAll(authProperties);
            producerProperties.putAll(authProperties);
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

            Awaitility
                .await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    assertThat(records)
                        .hasSize(1)
                        .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
                        .containsExactly(tuple(topicName, "testcontainers", "rulezzz"));
                });

            consumer.unsubscribe();
        }
    }

    protected static String getJaasConfig() {
        String jaasConfig =
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"admin\" " +
            "password=\"admin\" " +
            "user_admin=\"admin\" " +
            "user_test=\"secret\";";
        return jaasConfig;
    }
}
