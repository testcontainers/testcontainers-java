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

import static org.assertj.core.api.Assertions.assertThat;

class ActiveMQContainerTest {

    @Test
    void test() {
        try ( // container {
            ActiveMQContainer activemq = new ActiveMQContainer("apache/activemq-classic:5.18.3")
            // }
        ) {
            activemq.start();

            assertThat(activemq.getUser()).isNull();
            assertThat(activemq.getPassword()).isNull();
            assertFunctionality(activemq, false);
        }
    }

    @Test
    void customCredentials() {
        try (
            // settingCredentials {
            ActiveMQContainer activemq = new ActiveMQContainer("apache/activemq-classic:5.18.3")
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
        try (
            Connection connection = useCredentials
                ? connectionFactory.createConnection(activemq.getUser(), activemq.getPassword())
                : connectionFactory.createConnection()
        ) {
            connection.start();

            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
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
    }
}
