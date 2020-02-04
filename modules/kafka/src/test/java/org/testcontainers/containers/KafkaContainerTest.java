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
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;
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
            final String alias = kafka.getNetworkAliases().get(0);
            kafka.start();
            final RMISocketFactory socketFactory = RMISocketFactory.getDefaultSocketFactory();

            // Override RMISocketFactory because otherwise it will try to change the hostname to Kafka's internal host
            // https://github.com/testcontainers/testcontainers-java/pull/2192
            RMISocketFactory.setSocketFactory(new RMISocketFactory() {
                @Override
                public Socket createSocket(String host, int port) throws IOException {
                    if (alias.equals(host)) {
                        host = kafka.getContainerIpAddress();
                        port = kafka.getJmxServicePort();
                    }
                    return socketFactory.createSocket(host, port);
                }

                @Override
                public ServerSocket createServerSocket(int port) throws IOException {
                    return socketFactory.createServerSocket(port);
                }
            });

            String remoteJmxServiceUrl = kafka.getJmxServiceUrl();

            JMXConnector conn = JMXConnectorFactory.connect(new JMXServiceURL(remoteJmxServiceUrl));

            MBeanServerConnection mBeanServerConnection = conn.getMBeanServerConnection();

            assertThat(mBeanServerConnection.getDomains()).contains("kafka.server");
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
