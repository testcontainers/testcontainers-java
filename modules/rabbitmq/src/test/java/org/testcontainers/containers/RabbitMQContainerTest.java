package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of functionality special to the Neo4jContainer.
 *
 * @author Michael J. Simons
 */
public class RabbitMQContainerTest {

    // See org.testcontainers.utility.LicenseAcceptance#ACCEPTANCE_FILE_NAME
    private static final String ACCEPTANCE_FILE_LOCATION = "/container-license-acceptance.txt";
    public static final String DEFAULT_TAG = "3.7-management-alpine";
    public static final int DEFAULT_AMQPS_PORT = 5671;
    public static final int DEFAULT_AMQP_PORT = 5672;
    public static final int DEFAULT_HTTPS_PORT = 15671;
    public static final int DEFAULT_HTTP_PORT = 15672;

    @Test
    public void shouldCreateRabbitMQContainer() {
        RabbitMQContainer container = new RabbitMQContainer();

        assertThat(container.getDockerImageName()).isEqualTo("rabbitmq:" + DEFAULT_TAG);
        assertThat(container.getAdminPassword()).isEqualTo("guest");
        assertThat(container.getAdminUsername()).isEqualTo("guest");

        container.start();
        container.waitUntilContainerStarted();

        assertThat(container.getAmqpsUrl()).isEqualTo(
                String.format("amqps://%s:%d", container.getContainerIpAddress(), container.getMappedPort(DEFAULT_AMQPS_PORT)));
        assertThat(container.getAmqpUrl()).isEqualTo(
                String.format("amqp://%s:%d", container.getContainerIpAddress(), container.getMappedPort(DEFAULT_AMQP_PORT)));
        assertThat(container.getHttpsUrl()).isEqualTo(
                String.format("https://%s:%d", container.getContainerIpAddress(), container.getMappedPort(DEFAULT_HTTPS_PORT)));
        assertThat(container.getHttpUrl()).isEqualTo(
                String.format("http://%s:%d", container.getContainerIpAddress(), container.getMappedPort(DEFAULT_HTTP_PORT)));

        assertThat(container.getHttpsPort()).isEqualTo(container.getMappedPort(DEFAULT_HTTPS_PORT));
        assertThat(container.getHttpPort()).isEqualTo(container.getMappedPort(DEFAULT_HTTP_PORT));
        assertThat(container.getAmqpsPort()).isEqualTo(container.getMappedPort(DEFAULT_AMQPS_PORT));
        assertThat(container.getAmqpPort()).isEqualTo(container.getMappedPort(DEFAULT_AMQP_PORT));

        assertThat(container.getLivenessCheckPortNumbers()).containsExactlyInAnyOrder(
                container.getMappedPort(DEFAULT_AMQP_PORT),
                container.getMappedPort(DEFAULT_AMQPS_PORT),
                container.getMappedPort(DEFAULT_HTTP_PORT),
                container.getMappedPort(DEFAULT_HTTPS_PORT)
        );

    }

    @Test
    public void shouldCreateRabbitMQContainerWithTag() {
        RabbitMQContainer container = new RabbitMQContainer(DEFAULT_TAG);
        assertThat(container.getDockerImageName()).isEqualTo("rabbitmq:" + DEFAULT_TAG);
    }

    @Test
    public void shouldCreateRabbitMQContainerWithExchange() throws IOException, InterruptedException {
        RabbitMQContainer container =
                new RabbitMQContainer()
                        .withExchange("test-exchange", "direct");

        container.start();
        container.waitUntilContainerStarted();

        assertThat(container.execInContainer("rabbitmqctl", "list_exchanges").getStdout())
                .containsPattern("test-exchange\\s+direct");
        container.stop();
    }

    @Test
    public void shouldCreateRabbitMQContainerWithQueues() throws IOException, InterruptedException {
        RabbitMQContainer container = new RabbitMQContainer()
                .withQueue("queue-one")
                .withQueue("queue-two", false, true, ImmutableMap.of("x-message-ttl", 1000));

        container.start();
        container.waitUntilContainerStarted();

        assertThat(container.execInContainer("rabbitmqctl", "list_queues", "name", "arguments").getStdout())
                .containsPattern("queue-one");
        assertThat(container.execInContainer("rabbitmqctl", "list_queues", "name", "arguments").getStdout())
                .containsPattern("queue-two\\s.*x-message-ttl");
        container.stop();
    }

    @Test
    public void shouldMountConfigurationFile() {
        RabbitMQContainer container = new RabbitMQContainer("3.7-management-alpine")
                .withRabbitMQConfig(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
        container.start();
        container.waitUntilContainerStarted();

        assertThat(container.getLogs().contains("/etc/rabbitmq/rabbitmq-custom.conf")).isTrue();
        container.stop();
    }

    @Test
    public void shouldStartTheWholeEnchilada() throws IOException, InterruptedException {
        RabbitMQContainer container = new RabbitMQContainer()
                .withVhost("vhost1")
                .withVhostLimit("vhost1", "max-connections", 1)
                .withVhost("vhost2", true)
                .withExchange("direct-exchange", "direct")
                .withExchange("topic-exchange", "topic")
                .withQueue("queue1")
                .withQueue("queue2", true, false, ImmutableMap.of("x-message-ttl", 1000))
                .withBinding("direct-exchange", "queue1")
                .withUser("user1", "password1")
                .withUser("user2", "password2", ImmutableSet.of("administrator"))
                .withPermission("vhost1", "user1", ".*", ".*", ".*")
                .withPolicy("max length policy", "^dog", ImmutableMap.of("max-length", 1), 1, "queues")
                .withPolicy("alternate exchange policy", "^direct-exchange", ImmutableMap.of("alternate-exchange", "amq.direct"))
                .withOperatorPolicy("operator policy 1", "^queue1", ImmutableMap.of("message-ttl", 1000), 1, "queues")
                .withPluginsEnabled("rabbitmq_shovel", "rabbitmq_random_exchange");

        container.start();
        container.waitUntilContainerStarted();

        assertThat(container.execInContainer("rabbitmqadmin", "list", "queues")
                .getStdout())
                .contains("queue1", "queue2");

        assertThat(container.execInContainer("rabbitmqadmin", "list", "exchanges")
                .getStdout())
                .contains("direct-exchange", "topic-exchange");

        assertThat(container.execInContainer("rabbitmqadmin", "list", "bindings")
                .getStdout())
                .contains("direct-exchange");

        assertThat(container.execInContainer("rabbitmqadmin", "list", "users")
                .getStdout())
                .contains("user1", "user2");

        assertThat(container.execInContainer("rabbitmqadmin", "list", "policies")
                .getStdout())
                .contains("max length policy", "alternate exchange policy");

        assertThat(container.execInContainer("rabbitmqadmin", "list", "operator_policies")
                .getStdout())
                .contains("operator policy 1");

        assertThat(container.execInContainer("rabbitmq-plugins", "is_enabled", "rabbitmq_shovel")
                .getStdout())
                .contains("rabbitmq_shovel is enabled");

        assertThat(container.execInContainer("rabbitmq-plugins", "is_enabled", "rabbitmq_random_exchange")
                .getStdout())
                .contains("rabbitmq_random_exchange is enabled");

        container.stop();
    }
}
