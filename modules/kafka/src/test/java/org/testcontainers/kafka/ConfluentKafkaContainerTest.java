package org.testcontainers.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.junit.Test;
import org.testcontainers.AbstractKafka;
import org.testcontainers.KCatContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfluentKafkaContainerTest extends AbstractKafka {

    @Test
    public void testUsage() throws Exception {
        try ( // constructorWithVersion {
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
            // }
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testUsageWithListener() throws Exception {
        try (
            Network network = Network.newNetwork();
            // registerListener {
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
                .withListener("kafka:19092")
                .withNetwork(network);
            // }
            KCatContainer kcat = new KCatContainer().withNetwork(network)
        ) {
            kafka.start();
            kcat.start();

            kcat.execInContainer("kcat", "-b", "kafka:19092", "-t", "msgs", "-P", "-l", "/data/msgs.txt");
            String stdout = kcat
                .execInContainer("kcat", "-b", "kafka:19092", "-C", "-t", "msgs", "-c", "1")
                .getStdout();

            assertThat(stdout).contains("Message produced by kcat");
        }
    }

    @Test
    public void testUsageWithListenerFromProxy() throws Exception {
        try (
            Network network = Network.newNetwork();
            // registerListenerFromProxy {
            SocatContainer socat = new SocatContainer().withNetwork(network).withTarget(2000, "kafka", 19092);
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")
                .withListener("kafka:19092", () -> socat.getHost() + ":" + socat.getMappedPort(2000))
                .withNetwork(network)
            // }
        ) {
            socat.start();
            kafka.start();

            String bootstrapServers = String.format("%s:%s", socat.getHost(), socat.getMappedPort(2000));
            testKafkaFunctionality(bootstrapServers);
        }
    }

    @SneakyThrows
    @Test
    public void shouldConfigureAuthenticationWithSaslUsingJaas() {
        try (
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0")
                .withEnv(
                    "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                    "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT,CONTROLLER:PLAINTEXT"
                )
                .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("KAFKA_LISTENER_NAME_BROKER_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", getJaasConfig())
        ) {
            kafka.start();

            testSecurePlainKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @SneakyThrows
    @Test
    public void shouldConfigureAuthenticationWithSaslScramUsingJaas() {
        try (
            ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0") {
                @SneakyThrows
                @Override
                protected void containerIsStarting(InspectContainerResponse containerInfo) {
                    String command =
                        "echo 'kafka-storage format --ignore-formatted -t \"" +
                        "$CLUSTER_ID" +
                        "\" --add-scram SCRAM-SHA-256=[name=admin,password=admin] -c /etc/kafka/kafka.properties' >> /etc/confluent/docker/configure";
                    execInContainer("bash", "-c", command);
                    super.containerIsStarting(containerInfo);
                }
            }
                .withEnv(
                    "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                    "PLAINTEXT:SASL_PLAINTEXT,BROKER:SASL_PLAINTEXT,CONTROLLER:PLAINTEXT"
                )
                .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_SASL_ENABLED_MECHANISMS", "SCRAM-SHA-256")
                .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "SCRAM-SHA-256")
                .withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "SCRAM-SHA-256")
                .withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=/etc/kafka/secrets/kafka_server_jaas.conf")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("kafka_server_jaas.conf"),
                    "/etc/kafka/secrets/kafka_server_jaas.conf"
                )
        ) {
            kafka.start();

            testSecureScramKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
