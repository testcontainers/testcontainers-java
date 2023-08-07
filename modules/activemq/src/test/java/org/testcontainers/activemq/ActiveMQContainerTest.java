package org.testcontainers.activemq;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveMQContainerTest {

    @Test
    public void test() throws JMSException {
        try ( // container {
            ActiveMQContainer container = new ActiveMQContainer("apache/activemq-classic:5.17.5")
            // }
        ) {
            container.start();

            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(container.getBrokerUrl());
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
}
