package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;
import org.testcontainers.utility.MountableFile;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.containers.RabbitMQContainer.SslVerification.VERIFY_PEER;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

/**
 * @author Martin Greber
 */
public class RabbitMQContainerTest {

    public static final int DEFAULT_AMQPS_PORT = 5671;
    public static final int DEFAULT_AMQP_PORT = 5672;
    public static final int DEFAULT_HTTPS_PORT = 15671;
    public static final int DEFAULT_HTTP_PORT = 15672;

    @Test
    public void shouldCreateRabbitMQContainer()
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {

            assertThat(container.getAdminPassword()).isEqualTo("guest");
            assertThat(container.getAdminUsername()).isEqualTo("guest");

            container.start();

            assertThat(container.getAmqpsUrl()).isEqualTo(
                String.format("amqps://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_AMQPS_PORT)));
            assertThat(container.getAmqpUrl()).isEqualTo(
                String.format("amqp://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_AMQP_PORT)));
            assertThat(container.getHttpsUrl()).isEqualTo(
                String.format("https://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_HTTPS_PORT)));
            assertThat(container.getHttpUrl()).isEqualTo(
                String.format("http://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_HTTP_PORT)));

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
    }

    @Test
    public void shouldCreateRabbitMQContainerWithExchange() throws IOException, InterruptedException
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.withExchange("test-exchange", "direct");

            container.start();

            assertThat(container.execInContainer("rabbitmqctl", "list_exchanges").getStdout())
                .containsPattern("test-exchange\\s+direct");
        }
    }

    @Test
    public void shouldCreateRabbitMQContainerWithExchangeInVhost() throws IOException, InterruptedException
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.withVhost("test-vhost");
            container.withExchange("test-vhost", "test-exchange", "direct", false, false, false, Collections.emptyMap());

            container.start();

            assertThat(container.execInContainer("rabbitmqctl", "list_exchanges", "-p", "test-vhost").getStdout())
                .containsPattern("test-exchange\\s+direct");
        }
    }

    @Test
    public void shouldCreateRabbitMQContainerWithQueues() throws IOException, InterruptedException
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {

            container.withQueue("queue-one")
                .withQueue("queue-two", false, true, ImmutableMap.of("x-message-ttl", 1000));

            container.start();

            assertThat(container.execInContainer("rabbitmqctl", "list_queues", "name", "arguments").getStdout())
                .containsPattern("queue-one");
            assertThat(container.execInContainer("rabbitmqctl", "list_queues", "name", "arguments").getStdout())
                .containsPattern("queue-two\\s.*x-message-ttl");
        }
    }

    @Test
    public void shouldMountConfigurationFile()
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {

            container.withRabbitMQConfig(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
            container.start();

            assertThat(container.getLogs()).contains("config file(s) : /etc/rabbitmq/rabbitmq-custom.conf");
            assertThat(container.getLogs()).doesNotContain(" (not found)");
        }
    }


    @Test
    public void shouldMountConfigurationFileErlang()
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {

            container.withRabbitMQConfigErlang(MountableFile.forClasspathResource("/rabbitmq-custom.config"));
            container.start();

            assertThat(container.getLogs()).contains("config file(s) : /etc/rabbitmq/rabbitmq-custom.config");
            assertThat(container.getLogs()).doesNotContain(" (not found)");
        }
    }


    @Test
    public void shouldMountConfigurationFileSysctl()
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {

            container.withRabbitMQConfigSysctl(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
            container.start();

            assertThat(container.getLogs()).contains("config file(s) : /etc/rabbitmq/rabbitmq-custom.conf");
            assertThat(container.getLogs()).doesNotContain(" (not found)");
        }
    }

    @Test
    public void shouldStartTheWholeEnchilada() throws IOException, InterruptedException
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container
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
                    .withPolicy("vhost2", "ha-all", ".*", ImmutableMap.of("ha-mode", "all", "ha-sync-mode", "automatic"))
                    .withOperatorPolicy("operator policy 1", "^queue1", ImmutableMap.of("message-ttl", 1000), 1, "queues")
                    .withPluginsEnabled("rabbitmq_shovel", "rabbitmq_random_exchange");

            container.start();

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

            assertThat(container.execInContainer("rabbitmqadmin", "list", "policies", "--vhost=vhost2")
                    .getStdout())
                    .contains("ha-all", "ha-sync-mode");

            assertThat(container.execInContainer("rabbitmqadmin", "list", "operator_policies")
                .getStdout())
                .contains("operator policy 1");

            assertThat(container.execInContainer("rabbitmq-plugins", "is_enabled", "rabbitmq_shovel", "--quiet")
                .getStdout())
                .contains("rabbitmq_shovel is enabled");

            assertThat(container.execInContainer("rabbitmq-plugins", "is_enabled", "rabbitmq_random_exchange", "--quiet")
                .getStdout())
                .contains("rabbitmq_random_exchange is enabled");
        }
    }

    @Test
    public void shouldThrowExceptionForDodgyJson()
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {

            assertThatCode(() ->
                container.withQueue(
                    "queue2",
                    true,
                    false,
                    ImmutableMap.of("x-message-ttl", container))
            ).hasMessageStartingWith("Failed to convert arguments into json");

        }
    }

    @Test
    public void shouldWorkWithSSL()
    {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.withSSL(
                forClasspathResource("/certs/server_key.pem", 0644),
                forClasspathResource("/certs/server_certificate.pem", 0644),
                forClasspathResource("/certs/ca_certificate.pem", 0644),
                VERIFY_PEER,
                true
            );

            container.start();

            assertThatCode(() -> {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.useSslProtocol(createSslContext(
                    "certs/client_key.p12", "password",
                    "certs/truststore.jks", "password"));
                connectionFactory.enableHostnameVerification();
                connectionFactory.setUri(container.getAmqpsUrl());
                connectionFactory.setPassword(container.getAdminPassword());
                Connection connection = connectionFactory.newConnection();
                Channel channel = connection.openChannel().orElseThrow(() -> new RuntimeException("Failed to Open channel"));
                channel.close();
                connection.close();
            }).doesNotThrowAnyException();
        }
    }

    private SSLContext createSslContext(String keystoreFile, String keystorePassword, String truststoreFile, String truststorePassword)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException
    {
        ClassLoader classLoader = getClass().getClassLoader();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(new File(classLoader.getResource(keystoreFile).getFile())), keystorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "password".toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(new FileInputStream(new File(classLoader.getResource(truststoreFile).getFile())), truststorePassword.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        SSLContext c = SSLContext.getInstance("TLSv1.2");
        c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return c;
    }
}
