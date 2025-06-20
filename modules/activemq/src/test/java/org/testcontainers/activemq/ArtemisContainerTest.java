package org.testcontainers.activemq;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import lombok.SneakyThrows;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtemisContainerTest {

    @Test
    public void defaultCredentials() {
        try (
            // container {
            ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine")
            // }
        ) {
            artemis.start();

            assertThat(artemis.getUser()).isEqualTo("artemis");
            assertThat(artemis.getPassword()).isEqualTo("artemis");
            assertFunctionality(artemis, false);
        }
    }

    @Test
    public void customCredentials() {
        try (
            // settingCredentials {
            ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine")
                .withUser("testcontainers")
                .withPassword("testcontainers")
            // }
        ) {
            artemis.start();

            assertThat(artemis.getUser()).isEqualTo("testcontainers");
            assertThat(artemis.getPassword()).isEqualTo("testcontainers");
            assertFunctionality(artemis, false);
        }
    }

    @Test
    public void allowAnonymousLogin() {
        try (
            // enableAnonymousLogin {
            ArtemisContainer artemis = new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine")
                .withEnv("ANONYMOUS_LOGIN", "true")
            // }
        ) {
            artemis.start();

            assertFunctionality(artemis, true);
        }
    }

    @SneakyThrows
    private void assertFunctionality(ArtemisContainer artemis, boolean anonymousLogin) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(artemis.getBrokerUrl());
        if (!anonymousLogin) {
            connectionFactory.setUser(artemis.getUser());
            connectionFactory.setPassword(artemis.getPassword());
        }
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination destination = session.createQueue("test-queue");
        MessageProducer producer = session.createProducer(destination);

        String contentMessage = "Testcontainers";
        TextMessage message = session.createTextMessage(contentMessage);
        producer.send(message);

        MessageConsumer consumer = session.createConsumer(destination);
        TextMessage messageReceived = (TextMessage) consumer.receive();
        assertThat(messageReceived.getText()).isEqualTo(contentMessage);
    }
}
