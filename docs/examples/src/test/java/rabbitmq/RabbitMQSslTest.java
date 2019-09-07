package rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQSslTest {

    // ssl {
    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer()
        .withSSL(
            MountableFile.forClasspathResource("/rabbitmq/certs/server_key.pem", 0644),
            MountableFile.forClasspathResource("/rabbitmq/certs/server_certificate.pem", 0644),
            MountableFile.forClasspathResource("/rabbitmq/certs/ca_certificate.pem", 0644),
            RabbitMQContainer.SslVerification.VERIFY_PEER,
            true);
    // }

    @Test
    public void test() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException, CertificateException, KeyStoreException, UnrecoverableKeyException {
        assertTrue(rabbitmq.isRunning()); // good enough to check that the container started listening

        // connection {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useSslProtocol(createSslContext(
            "rabbitmq/certs/client_key.p12", "password",
            "rabbitmq/certs/truststore.jks", "password"));
        connectionFactory.enableHostnameVerification();
        connectionFactory.setUri(rabbitmq.getAmqpsUrl());
        connectionFactory.setPassword(rabbitmq.getAdminPassword());
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.openChannel().orElseThrow(() -> new RuntimeException("Failed to Open channel"))) {
            assertThat(channel.isOpen()).isTrue();
        }
        // }

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
