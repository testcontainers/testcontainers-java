package org.testcontainers.solace;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SolaceContainerSMFTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceContainerSMFTest.class);

    private static final String MESSAGE = "HelloWorld";

    private static final Topic TOPIC = JCSMPFactory.onlyInstance().createTopic("Topic/ActualTopic");

    @Test
    public void testSolaceContainerWithSimpleAuthentication() {
        try (
            // solaceContainerSetup {
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.2")
                .withCredentials("user", "pass")
                .withTopic("Topic/ActualTopic", Service.SMF)
                .withVpn("test_vpn")
            // }
        ) {
            solaceContainer.start();
            JCSMPSession session = createSessionWithBasicAuth(solaceContainer);
            Assertions.assertThat(session).isNotNull();
            Assertions.assertThat(consumeMessageFromSolace(session)).isEqualTo(MESSAGE);
            session.closeSession();
        }
    }

    @Test
    public void testSolaceContainerWithCertificates() {
        try (
            // solaceContainerUsageSSL {
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.2")
                .withClientCert(
                    MountableFile.forClasspathResource("solace.pem"),
                    MountableFile.forClasspathResource("rootCA.crt")
                )
                .withTopic("Topic/ActualTopic", Service.SMF_SSL)
            // }
        ) {
            solaceContainer.start();
            JCSMPSession session = createSessionWithCertificates(solaceContainer);
            Assertions.assertThat(session).isNotNull();
            Assertions.assertThat(consumeMessageFromSolace(session)).isEqualTo(MESSAGE);
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

    private void publishMessageToSolace(JCSMPSession session) throws JCSMPException {
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

    private String consumeMessageFromSolace(JCSMPSession session) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            String[] result = new String[1];
            XMLMessageConsumer cons = session.getMessageConsumer(
                new XMLMessageListener() {
                    @Override
                    public void onReceive(BytesXMLMessage msg) {
                        if (msg instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) msg;
                            String message = textMessage.getText();
                            result[0] = message;
                            LOGGER.info("TextMessage received: " + message);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onException(JCSMPException e) {
                        LOGGER.error("Exception received: " + e.getMessage());
                        latch.countDown();
                    }
                }
            );
            session.addSubscription(TOPIC);
            cons.start();
            publishMessageToSolace(session);
            Assertions.assertThat(latch.await(10L, TimeUnit.SECONDS)).isTrue();
            return result[0];
        } catch (Exception e) {
            throw new RuntimeException("Cannot receive message from solace", e);
        }
    }
}
