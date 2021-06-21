package org.testcontainers.containers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.utility.MountableFile;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RabbitMQRunningDocuExamplesTest {

    // createConfiguredContainer {
    @ClassRule
    public static final RabbitMQContainer configuredContainer = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine")
        .withUser("Username", "Password")
        .withQueue("test-queue")
        .withExchange("test.exchange", "topic")
        .withBinding("test.exchange", "test-queue", new HashMap<>(), "routing.key.foo.bar", "queue");
    // }

    @Test
    public void checkConfiguredContainer() throws Exception {
        // Check that User exists
        assertThat(configuredContainer.execInContainer("rabbitmqadmin", "list", "users")
            .getStdout())
            .contains("Username");

        // Check that Queue exists
        assertThat(configuredContainer.execInContainer("rabbitmqadmin", "list", "queues")
            .getStdout())
            .contains("test-queue");

        // Check that exchange exists
        assertThat(configuredContainer.execInContainer("rabbitmqadmin", "list", "exchanges")
            .getStdout())
            .contains("test.exchange");

        // Check that binding exists
        assertThat(configuredContainer.execInContainer("rabbitmqadmin", "list", "bindings")
            .getStdout())
            .contains("test.exchange");
    }


    // createContainerWithConfigFile {
    @ClassRule
    public static final RabbitMQContainer containerWithRabbitMQConfig = new RabbitMQContainer("rabbitmq:3.8.9-management")
        .withRabbitMQConfig(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));

    // or

    @ClassRule
    public static final RabbitMQContainer containerWithSysctlConfig = new RabbitMQContainer("rabbitmq:3.8.9-management")
        .withRabbitMQConfigSysctl(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));

    // or

    @ClassRule
    public static final RabbitMQContainer containerWithErlangConfig = new RabbitMQContainer("rabbitmq:3.8.9-management")
        .withRabbitMQConfigErlang(MountableFile.forClasspathResource("/rabbitmq-custom.config"));
    // }

    @Test
    public void checkThatFileConfiguredContainersAreStarted() {
        assertTrue(containerWithRabbitMQConfig.isRunning());
        assertTrue(containerWithSysctlConfig.isRunning());
        assertTrue(containerWithErlangConfig.isRunning());
    }


    // createContainer {
    @ClassRule
    public static final RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.8.9-management");
    // }

    @Test
    public void basicConnectionPropertiesTest() {
        // getConnectionProperties {
        final String amqpUrl = container.getAmqpUrl(); // will provide the connection string 'amqp://<container-ip>:<mapped-amqp-port>'
        final String managementUrl = container.getHttpUrl(); // will provide the connection string 'http://<container-ip>:<mapped-management-port>'
        final String adminUsername = container.getAdminUsername();
        final String adminPassword = container.getAdminPassword();
        // }

        // Check that connection can be established to RabbitMQ using properties mentioned above
        assertThatCode(() -> {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.enableHostnameVerification();
            connectionFactory.setUri(amqpUrl);
            connectionFactory.setUsername(adminUsername);
            connectionFactory.setPassword(adminPassword);
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.openChannel().orElseThrow(() -> new RuntimeException("Failed to Open channel"));
            channel.close();
            connection.close();
        }).doesNotThrowAnyException();

        // Check that management URL is correctly build and returned
        assertEquals(managementUrl, "http://" + container.getContainerIpAddress() + ":" + container.getMappedPort(15672));
    }

    // sslUsageExample {
    @ClassRule
    public static final RabbitMQContainer containerWithSSL = new RabbitMQContainer("rabbitmq:3.8.9-management")
        .withSSL(
            MountableFile.forClasspathResource("/certs/server_key.pem", 0644),
            MountableFile.forClasspathResource("/certs/server_certificate.pem", 0644),
            MountableFile.forClasspathResource("/certs/ca_certificate.pem", 0644),
            RabbitMQContainer.SslVerification.VERIFY_PEER,
            true
        );

    @Test
    public void sslConnectionTest() {
        final String superSecureAmqpsUrl = containerWithSSL.getAmqpsUrl();
        final String superSecureManagementUrl = containerWithSSL.getHttpsUrl();

        // setup your super secure SSL connection...
        // }
        assertThatCode(() -> {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.useSslProtocol(new RabbitMQSSLContextHelper().createSslContext(
                "certs/client_key.p12", "password",
                "certs/truststore.jks", "password"));
            connectionFactory.enableHostnameVerification();
            connectionFactory.setUri(superSecureAmqpsUrl);
            connectionFactory.setPassword(containerWithSSL.getAdminPassword());
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.openChannel().orElseThrow(() -> new RuntimeException("Failed to Open channel"));
            channel.close();
            connection.close();
        }).doesNotThrowAnyException();

        // Check that management URL is correctly build and returned
        assertEquals(superSecureManagementUrl, "https://" + containerWithSSL.getContainerIpAddress() + ":" + containerWithSSL.getMappedPort(15671));
    }
}
