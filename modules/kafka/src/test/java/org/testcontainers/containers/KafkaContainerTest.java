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

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class KafkaContainerTest {

    private final static String TOPIC_NAME = "messages";

    @Test
    public void testUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer()) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
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
    public void testKafkaJmxAccess() throws Exception {
        try (
            KafkaContainer kafka = new KafkaContainer()
                .withRemoteJmxService()
        ) {
            kafka.start();
            String remoteJmxServiceUrl = kafka.getJmxServiceUrl();

            JMXServiceURL url = new JMXServiceURL(remoteJmxServiceUrl);
            JMXConnector conn = JMXConnectorFactory.connect(url);
            MBeanServerConnection mBeanServerConnection = conn.getMBeanServerConnection();

            assertThat(mBeanServerConnection.getMBeanCount()).isGreaterThan(0);

            testKafkaFunctionality(kafka.getBootstrapServers());

            // assert bytes in for topic is greater than 0 after some data was produced
            ObjectName bytesInPerSec = new ObjectName(String.format("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=%s", TOPIC_NAME));
            String[] attributes = {"Count"};

            AttributeList attributeValues = mBeanServerConnection.getAttributes(bytesInPerSec, attributes);

            assertThat(attributes.length).isEqualTo(attributeValues.size());

            Attribute attr = (Attribute) attributeValues.stream().findFirst().get();
            Long val = (Long)attr.getValue();
            assertThat(val).isGreaterThan(0);
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

            consumer.subscribe(Arrays.asList(TOPIC_NAME));

            producer.send(new ProducerRecord<>(TOPIC_NAME, "testcontainers", "rulezzz")).get();

            Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
                ConsumerRecords<String, String> records = consumer.poll(100);

                if (records.isEmpty()) {
                    return false;
                }

                assertThat(records)
                    .hasSize(1)
                    .extracting(ConsumerRecord::topic, ConsumerRecord::key, ConsumerRecord::value)
                    .containsExactly(tuple(TOPIC_NAME, "testcontainers", "rulezzz"));

                return true;
            });

            consumer.unsubscribe();
        }
    }

}
