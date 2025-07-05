package org.testcontainers.solace;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SolaceContainerSMFTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceContainerSMFTest.class);

    private static final String MESSAGE = "HelloWorld";

    private static final Topic TOPIC = JCSMPFactory.onlyInstance().createTopic("Topic/ActualTopic");

    private static final Queue QUEUE = JCSMPFactory.onlyInstance().createQueue("Queue");

    @Test
    public void testSolaceContainerWithSimpleAuthentication() {
        try (
            // solaceContainerSetup {
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.25.0")
                .withCredentials("user", "pass")
                .withTopic(TOPIC.getName(), Service.SMF)
                .withVpn("test_vpn")
            // }
        ) {
            solaceContainer.start();
            JCSMPSession session = createSessionWithBasicAuth(solaceContainer);
            assertThat(session).isNotNull();
            consumeMessageFromTopics(session);
            session.closeSession();
        }
    }

    @Test
    public void testSolaceContainerWithCreateFlow() {
        try (
            // solaceContainerSetup {
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.25.0")
                .withCredentials("user", "pass")
                .withTopic(TOPIC.getName(), Service.SMF)
                .withVpn("test_vpn")
            // }
        ) {
            solaceContainer.start();
            JCSMPSession session = createSessionWithBasicAuth(solaceContainer);
            assertThat(session).isNotNull();
            testCreateFlow(session);
            session.closeSession();
        }
    }

    private static void testCreateFlow(JCSMPSession session) {
        try {
            EndpointProperties endpointProperties = new EndpointProperties();
            endpointProperties.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
            endpointProperties.setQuota(1000);
            session.provision(QUEUE, endpointProperties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
            session.addSubscription(QUEUE, TOPIC, JCSMPSession.WAIT_FOR_CONFIRM);
            ConsumerFlowProperties flowProperties = new ConsumerFlowProperties().setEndpoint(QUEUE);
            TestConsumer listener = new TestConsumer();
            session.createFlow(listener, flowProperties).start();
            publishMessageToSolaceTopic(session);
            listener.waitForMessage();
        } catch (Exception e) {
            throw new RuntimeException("Cannot process message using solace topic/queue: " + e.getMessage(), e);
        }
    }

    @Test
    public void testSolaceContainerWithCertificates() {
        try (
            // solaceContainerUsageSSL {
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.25.0")
                .withClientCert(
                    MountableFile.forClasspathResource("solace.pem"),
                    MountableFile.forClasspathResource("rootCA.crt")
                )
                .withTopic(TOPIC.getName(), Service.SMF_SSL)
            // }
        ) {
            solaceContainer.start();
            JCSMPSession session = createSessionWithCertificates(solaceContainer);
            assertThat(session).isNotNull();
            consumeMessageFromTopics(session);
            session.closeSession();
        }
    }

    private String getResourceFileLocation(String name) {
        return getClass().getClassLoader().getResource(name).getPath();
    }

    private static JCSMPSession createSessionWithBasicAuth(SolaceContainer solace) {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solace.getOrigin(Service.SMF));
        properties.setProperty(JCSMPProperties.VPN_NAME, solace.getVpn());
        properties.setProperty(JCSMPProperties.USERNAME, solace.getUsername());
        properties.setProperty(JCSMPProperties.PASSWORD, solace.getPassword());
        return createSession(properties);
    }

    private JCSMPSession createSessionWithCertificates(SolaceContainer solace) {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solace.getOrigin(Service.SMF_SSL));
        properties.setProperty(JCSMPProperties.VPN_NAME, solace.getVpn());
        properties.setProperty(JCSMPProperties.USERNAME, solace.getUsername());
        // Just for testing purposes
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_HOST, false);
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, true);
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, true);
        properties.setProperty(
            JCSMPProperties.AUTHENTICATION_SCHEME,
            JCSMPProperties.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE
        );
        properties.setProperty(JCSMPProperties.SSL_TRUST_STORE, getResourceFileLocation("truststore"));
        properties.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, "solace");
        properties.setProperty(JCSMPProperties.SSL_KEY_STORE, getResourceFileLocation("client.pfx"));
        properties.setProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD, "solace");
        return createSession(properties);
    }

    private static JCSMPSession createSession(JCSMPProperties properties) {
        try {
            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();
            return session;
        } catch (Exception e) {
            Assert.fail("Error connecting and setting up session! " + e.getMessage());
            return null;
        }
    }

    private static void publishMessageToSolaceTopic(JCSMPSession session) throws JCSMPException {
        XMLMessageProducer producer = session.getMessageProducer(
            new JCSMPStreamingPublishCorrelatingEventHandler() {
                @Override
                public void responseReceivedEx(Object o) {
                    LOGGER.info("Producer received response for msg: " + o);
                }

                @Override
                public void handleErrorEx(Object o, JCSMPException e, long l) {
                    LOGGER.error(String.format("Producer received error for msg: %s - %s", o, e));
                }
            }
        );
        TextMessage msg = producer.createTextMessage();
        msg.setText(MESSAGE);
        producer.send(msg, TOPIC);
    }

    private static void consumeMessageFromTopics(JCSMPSession session) {
        try {
            TestConsumer listener = new TestConsumer();
            XMLMessageConsumer cons = session.getMessageConsumer(listener);
            session.addSubscription(TOPIC);
            cons.start();
            publishMessageToSolaceTopic(session);
            listener.waitForMessage();
        } catch (Exception e) {
            throw new RuntimeException("Cannot process message using solace: " + e.getMessage(), e);
        }
    }

    static class TestConsumer implements XMLMessageListener {

        private final CountDownLatch latch = new CountDownLatch(1);

        private String result;

        @Override
        public void onReceive(BytesXMLMessage msg) {
            if (msg instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) msg;
                String message = textMessage.getText();
                result = message;
                LOGGER.info("Message received: " + message);
            }
            latch.countDown();
        }

        @Override
        public void onException(JCSMPException e) {
            LOGGER.error("Exception received: " + e.getMessage());
            latch.countDown();
        }

        private void waitForMessage() {
            try {
                assertThat(latch.await(10L, TimeUnit.SECONDS)).isTrue();
                assertThat(result).isEqualTo(MESSAGE);
            } catch (Exception e) {
                throw new RuntimeException("Cannot receive message from solace: " + e.getMessage(), e);
            }
        }
    }
}
