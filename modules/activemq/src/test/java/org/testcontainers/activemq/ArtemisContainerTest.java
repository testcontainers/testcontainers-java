package org.testcontainers.activemq;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import lombok.SneakyThrows;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtemisContainerTest {

    @Test
    public void defaultCredentials() {
        try ( // container {
            ArtemisContainer container = new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine")
            // }
        ) {
            container.start();

            assertThat(container.getUser()).isEqualTo("artemis");
            assertThat(container.getPassword()).isEqualTo("artemis");
            assertFunctionality(container, false);
        }
    }

    @Test
    public void customCredentials() {
        try ( // settingCredentials {
            ArtemisContainer container = new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine")
                .withUser("testcontainers")
                .withPassword("testcontainers")
            // }
        ) {
            container.start();

            assertThat(container.getUser()).isEqualTo("testcontainers");
            assertThat(container.getPassword()).isEqualTo("testcontainers");
            assertFunctionality(container, false);
        }
    }

    @Test
    public void allowAnonymousLogin() {
        try (
            // anonymousLogin {
            ArtemisContainer container = new ArtemisContainer("apache/activemq-artemis:2.30.0-alpine")
                .withEnv("ANONYMOUS_LOGIN", "true")
            // }
        ) {
            container.start();

            assertFunctionality(container, true);
        }
    }

    @SneakyThrows
    private void assertFunctionality(ArtemisContainer container, boolean anonymousLogin) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(container.getBrokerUrl());
        if (!anonymousLogin) {
            connectionFactory.setUser(container.getUser());
            connectionFactory.setPassword(container.getPassword());
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
