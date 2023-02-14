package org.testcontainers.solace;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
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

public class SolaceContainerAMQPTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceContainerAMQPTest.class);

    private static final String MESSAGE = "HelloWorld";

    private static final String TOPIC_NAME = "Topic/ActualTopic";

    @Test
    public void testSolaceContainer() throws JMSException {
        try (
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.2")
                .withTopic(TOPIC_NAME, Service.AMQP)
                .withVpn("amqp-vpn")
        ) {
            solaceContainer.start();
            // solaceContainerUsage {
            Session session = createSession(
                solaceContainer.getUsername(),
                solaceContainer.getPassword(),
                solaceContainer.getOrigin(Service.AMQP)
            );
            // }
            Assertions.assertThat(session).isNotNull();
            Assertions.assertThat(consumeMessageFromSolace(session)).isEqualTo(MESSAGE);
            session.close();
        }
    }

    private static Session createSession(String username, String password, String host) {
        try {
            ConnectionFactory connectionFactory = new JmsConnectionFactory(username, password, host);
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession();
            connection.start();
            return session;
        } catch (Exception e) {
            Assert.fail("Error connecting and setting up session! " + e.getMessage());
            return null;
        }
    }

    private void publishMessageToSolace(Session session, Topic topic) throws JMSException {
        MessageProducer messageProducer = session.createProducer(topic);
        TextMessage message = session.createTextMessage(MESSAGE);
        messageProducer.send(message);
        messageProducer.close();
    }

    private String consumeMessageFromSolace(Session session) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            String[] result = new String[1];
            Topic topic = session.createTopic(TOPIC_NAME);
            MessageConsumer messageConsumer = session.createConsumer(topic);
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
            Assertions.assertThat(latch.await(10L, TimeUnit.SECONDS)).isTrue();
            messageConsumer.close();
            return result[0];
        } catch (Exception e) {
            throw new RuntimeException("Cannot receive message from solace", e);
        }
    }
}
