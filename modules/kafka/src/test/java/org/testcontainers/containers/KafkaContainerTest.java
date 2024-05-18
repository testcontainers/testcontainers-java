package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.testcontainers.AbstractKafka;
import org.testcontainers.Testcontainers;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.SystemStubs;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KafkaContainerTest extends AbstractKafka {

    private static final DockerImageName KAFKA_TEST_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:6.2.1");

    private static final DockerImageName KAFKA_KRAFT_TEST_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.0.1");

    private static final DockerImageName ZOOKEEPER_TEST_IMAGE = DockerImageName.parse(
        "confluentinc/cp-zookeeper:4.0.0"
    );

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
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
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
        try (KafkaContainer kafka = new KafkaContainer("6.2.1")) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testExternalZookeeperWithExternalNetwork() throws Exception {
        try (
            Network network = Network.newNetwork();
            // withExternalZookeeper {
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
                .withNetwork(network)
                .withExternalZookeeper("zookeeper:2181");
            // }

            GenericContainer<?> zookeeper = new GenericContainer<>(ZOOKEEPER_TEST_IMAGE)
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
    public void testConfluentPlatformVersion7() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.2.2"))) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testConfluentPlatformVersion5() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testWithHostExposedPort() throws Exception {
        Testcontainers.exposeHostPorts(12345);
        try (KafkaContainer kafka = new KafkaContainer(KAFKA_TEST_IMAGE)) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testWithHostExposedPortAndExternalNetwork() throws Exception {
        Testcontainers.exposeHostPorts(12345);
        try (KafkaContainer kafka = new KafkaContainer(KAFKA_TEST_IMAGE).withNetwork(Network.newNetwork())) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testUsageKraftBeforeConfluentPlatformVersion74() throws Exception {
        try (
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1")).withKraft()
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testUsageKraftAfterConfluentPlatformVersion74() throws Exception {
        try (
            // withKraftMode {
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).withKraft()
            // }
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testNotSupportedKraftVersion() {
        try (
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")).withKraft()
        ) {} catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                .isEqualTo(
                    "Provided Confluent Platform's version 6.2.1 is not supported in Kraft mode (must be 7.0.0 or above)"
                );
        }
    }

    @Test
    public void testKraftZookeeperMutualExclusion() {
        try (
            KafkaContainer kafka = new KafkaContainer(KAFKA_KRAFT_TEST_IMAGE).withKraft().withExternalZookeeper("")
        ) {} catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Cannot configure Zookeeper when using Kraft mode");
        }

        try (
            KafkaContainer kafka = new KafkaContainer(KAFKA_KRAFT_TEST_IMAGE).withExternalZookeeper("").withKraft()
        ) {} catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Cannot configure Kraft mode when Zookeeper configured");
        }

        try (
            KafkaContainer kafka = new KafkaContainer(KAFKA_KRAFT_TEST_IMAGE).withKraft().withEmbeddedZookeeper()
        ) {} catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Cannot configure Zookeeper when using Kraft mode");
        }
    }

    @Test
    public void testKraftPrecedenceOverEmbeddedZookeeper() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(KAFKA_KRAFT_TEST_IMAGE).withEmbeddedZookeeper().withKraft()) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testUsageWithListener() throws Exception {
        try (
            Network network = Network.newNetwork();
            // registerListener {
            KafkaContainer kafka = new KafkaContainer(KAFKA_KRAFT_TEST_IMAGE)
                .withListener(() -> "kafka:19092")
                .withNetwork(network);
            // }
            // createKCatContainer {
            GenericContainer<?> kcat = new GenericContainer<>("confluentinc/cp-kcat:7.4.1")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sh");
                })
                .withCopyToContainer(Transferable.of("Message produced by kcat"), "/data/msgs.txt")
                .withNetwork(network)
                .withCommand("-c", "tail -f /dev/null")
            // }
        ) {
            kafka.start();
            kcat.start();
            // produceConsumeMessage {
            kcat.execInContainer("kcat", "-b", "kafka:19092", "-t", "msgs", "-P", "-l", "/data/msgs.txt");
            String stdout = kcat
                .execInContainer("kcat", "-b", "kafka:19092", "-C", "-t", "msgs", "-c", "1")
                .getStdout();
            // }
            assertThat(stdout).contains("Message produced by kcat");
        }
    }

    @SneakyThrows
    @Test
    public void shouldConfigureAuthenticationWithSaslUsingJaas() {
        try (
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT")
                .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
        ) {
            kafka.start();

            testSecureKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @SneakyThrows
    @Test
    public void enableSaslWithUnsuccessfulTopicCreation() {
        try (
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT")
                .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
                .withEnv("KAFKA_AUTHORIZER_CLASS_NAME", "kafka.security.authorizer.AclAuthorizer")
                .withEnv("KAFKA_SUPER_USERS", "User:admin")
        ) {
            kafka.start();

            AdminClient adminClient = AdminClient.create(
                ImmutableMap.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafka.getBootstrapServers(),
                    AdminClientConfig.SECURITY_PROTOCOL_CONFIG,
                    "SASL_PLAINTEXT",
                    SaslConfigs.SASL_MECHANISM,
                    "PLAIN",
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"secret\";"
                )
            );

            String topicName = "messages-" + UUID.randomUUID();
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, 1, (short) 1));

            Awaitility
                .await()
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(TopicAuthorizationException.class);
                });
        }
    }

    @SneakyThrows
    @Test
    public void enableSaslAndWithAuthenticationError() {
        String jaasConfig = getJaasConfig();
        try (
            KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT")
                .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", jaasConfig)
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", jaasConfig)
        ) {
            kafka.start();

            AdminClient adminClient = AdminClient.create(
                ImmutableMap.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafka.getBootstrapServers(),
                    AdminClientConfig.SECURITY_PROTOCOL_CONFIG,
                    "SASL_PLAINTEXT",
                    SaslConfigs.SASL_MECHANISM,
                    "PLAIN",
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"test\" password=\"secretx\";"
                )
            );

            String topicName = "messages-" + UUID.randomUUID();
            Collection<NewTopic> topics = Collections.singletonList(new NewTopic(topicName, 1, (short) 1));

            Awaitility
                .await()
                .untilAsserted(() -> {
                    assertThatThrownBy(() -> adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(SaslAuthenticationException.class);
                });
        }
    }

    @Test
    public void starterScriptCanBeOverriddenWithEnvironmentVariable() throws Exception {
        SystemStubs
            .withEnvironmentVariables("TESTCONTAINERS_KAFKA_STARTER_SCRIPT_OVERRIDE", "/tmp/testcontainers_start.sh")
            .execute(() -> {
                try (KafkaContainer kafka = new KafkaContainer(KAFKA_TEST_IMAGE)) {
                    kafka.start();
                    testKafkaFunctionality(kafka.getBootstrapServers());
                    assertThat(kafka.execInContainer("ls", "/tmp/").getStdout()).contains("testcontainers_start.sh");
                }
            });
    }

    private static String getJaasConfig() {
        String jaasConfig =
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"admin\" " +
            "password=\"admin\" " +
            "user_admin=\"admin\" " +
            "user_test=\"secret\";";
        return jaasConfig;
    }
}
