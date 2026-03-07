package org.testcontainers.activemq;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import lombok.SneakyThrows;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveMQContainerTest {

    @Test
    void test() {
        try ( // container {
            ActiveMQContainer activemq = new ActiveMQContainer("apache/activemq:5.18.7")
            // }
        ) {
            activemq.start();

            assertThat(activemq.getUser()).isNull();
            assertThat(activemq.getPassword()).isNull();
            assertFunctionality(activemq, false);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "apache/activemq-classic:5.18.7", "apache/activemq:5.18.7" })
    void compatibility(String image) {
        try (ActiveMQContainer activemq = new ActiveMQContainer(image)) {
            activemq.start();
            assertFunctionality(activemq, false);
        }
    }

    @Test
    void customCredentials() {
        try (
            // settingCredentials {
            ActiveMQContainer activemq = new ActiveMQContainer("apache/activemq:5.18.7")
                .withUser("testcontainers")
                .withPassword("testcontainers")
            // }
        ) {
            activemq.start();

            assertThat(activemq.getUser()).isEqualTo("testcontainers");
            assertThat(activemq.getPassword()).isEqualTo("testcontainers");
            assertFunctionality(activemq, true);
        }
    }

    @SneakyThrows
    private void assertFunctionality(ActiveMQContainer activemq, boolean useCredentials) {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(activemq.getBrokerUrl());
        Connection connection;
        if (useCredentials) {
            connection = connectionFactory.createConnection(activemq.getUser(), activemq.getPassword());
        } else {
            connection = connectionFactory.createConnection();
        }
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
