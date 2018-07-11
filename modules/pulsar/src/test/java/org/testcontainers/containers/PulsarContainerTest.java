package org.testcontainers.containers;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class PulsarContainerTest {

    public static final String TEST_TOPIC = "test_topic";

    @Test
    public void testUsage() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer()) {
            pulsar.start();
            testPulsarFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    protected void testPulsarFunctionality(String pulsarBrokerUrl) throws Exception {

        try (
            PulsarClient client = PulsarClient.builder()
                .serviceUrl(pulsarBrokerUrl)
                .build();
            Consumer consumer = client.newConsumer()
                .topic(TEST_TOPIC)
                .subscriptionName("test-subs")
                .subscribe();
            Producer<byte[]> producer = client.newProducer()
                .topic(TEST_TOPIC)
                .create()
        ) {

            producer.send("test containers".getBytes());
            CompletableFuture<Message> future = consumer.receiveAsync();
            Message message = future.get(5, TimeUnit.SECONDS);

            assertThat(new String(message.getData()))
                .isEqualTo("test containers");
        }
    }

}
