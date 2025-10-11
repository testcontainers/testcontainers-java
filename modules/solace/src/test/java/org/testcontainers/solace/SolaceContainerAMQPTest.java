package org.testcontainers.solace;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import static org.assertj.core.api.Assertions.assertThat;

class SolaceContainerAMQPTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceContainerAMQPTest.class);

    private static final String MESSAGE = "HelloWorld";

    private static final String TOPIC_NAME = "Topic/ActualTopic";

    @Test
    void testSolaceContainer() throws JMSException, InterruptedException {
        try (
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.25.0")
                .withTopic(TOPIC_NAME, Service.AMQP)
                .withVpn("amqp-vpn")
        ) {
            solaceContainer.start();
            // solaceContainerUsage {
            ConnectionFactory connectionFactory = new JmsConnectionFactory(
                solaceContainer.getUsername(),
                solaceContainer.getPassword(),
                solaceContainer.getOrigin(Service.AMQP)
            );
            try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession()
            ) {
                // }
                connection.start();
                assertThat(session).isNotNull();
                assertThat(consumeMessageFromSolace(session)).isEqualTo(MESSAGE);
            }
        }
    }

    private void publishMessageToSolace(Session session, Topic topic) throws JMSException {
        try (MessageProducer messageProducer = session.createProducer(topic)) {
            TextMessage message = session.createTextMessage(MESSAGE);
            messageProducer.send(message);
        }
    }

    private String consumeMessageFromSolace(Session session) throws JMSException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        String[] result = new String[1];
        Topic topic = session.createTopic(TOPIC_NAME);
        try (MessageConsumer messageConsumer = session.createConsumer(topic)) {
            messageConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        result[0] = ((TextMessage) message).getText();
                    }
                    latch.countDown();
                } catch (Exception e) {
                    LOGGER.error("Exception received: " + e.getMessage());
                    latch.countDown();
                }
            });
            publishMessageToSolace(session, topic);
            assertThat(latch.await(10L, TimeUnit.SECONDS)).isTrue();
            messageConsumer.close();
            return result[0];
        }
    }
}
