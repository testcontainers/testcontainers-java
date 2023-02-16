package org.testcontainers.solace;

import org.assertj.core.api.Assertions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SolaceContainerMQTTTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceContainerMQTTTest.class);

    private static final String MESSAGE = "HelloWorld";

    private static final String TOPIC_NAME = "Topic/ActualTopic";

    @Test
    public void testSolaceContainer() {
        try (
            SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.2")
                .withTopic(TOPIC_NAME, Service.MQTT)
                .withVpn("mqtt-vpn")
        ) {
            solaceContainer.start();
            MqttClient client = createClient(
                solaceContainer.getUsername(),
                solaceContainer.getPassword(),
                solaceContainer.getOrigin(Service.MQTT)
            );
            Assertions.assertThat(client).isNotNull();
            Assertions.assertThat(consumeMessageFromSolace(client)).isEqualTo(MESSAGE);
        }
    }

    private static MqttClient createClient(String username, String password, String host) {
        try {
            MqttClient mqttClient = new MqttClient(host, MESSAGE);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            mqttClient.connect(connOpts);
            return mqttClient;
        } catch (Exception e) {
            Assert.fail("Error connecting and setting up session! " + e.getMessage());
            return null;
        }
    }

    private void publishMessageToSolace(MqttClient mqttClient) throws MqttException {
        MqttMessage message = new MqttMessage(MESSAGE.getBytes());
        message.setQos(0);
        mqttClient.publish(TOPIC_NAME, message);
    }

    private String consumeMessageFromSolace(MqttClient client) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            String[] result = new String[1];
            client.setCallback(
                new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        LOGGER.error("Exception received: " + cause.getMessage());
                        latch.countDown();
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        result[0] = new String(message.getPayload());
                        latch.countDown();
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                }
            );
            client.subscribe(TOPIC_NAME, 0);
            publishMessageToSolace(client);
            Assertions.assertThat(latch.await(10L, TimeUnit.SECONDS)).isTrue();
            return result[0];
        } catch (Exception e) {
            throw new RuntimeException("Cannot receive message from solace", e);
        }
    }
}
