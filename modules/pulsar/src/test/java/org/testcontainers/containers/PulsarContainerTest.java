package org.testcontainers.containers;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PulsarContainerTest {

    public static final String TEST_TOPIC = "test_topic";
    private static final DockerImageName PULSAR_IMAGE = DockerImageName.parse("apachepulsar/pulsar:2.2.0");

    @Test
    public void testUsage() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE)) {
            pulsar.start();
            testPulsarFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    @Test
    public void shouldNotEnableFunctionsWorkerByDefault() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer("2.5.1")) {
            pulsar.start();

            PulsarAdmin pulsarAdmin = PulsarAdmin.builder()
                .serviceHttpUrl(pulsar.getHttpServiceUrl())
                .build();

            assertThatThrownBy(() -> pulsarAdmin.functions().getFunctions("public", "default"))
                .isInstanceOf(PulsarAdminException.class);
        }
    }

    @Test
    public void shouldWaitForFunctionsWorkerStarted() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer("2.5.1").withFunctionsWorker()) {
            pulsar.start();

            PulsarAdmin pulsarAdmin = PulsarAdmin.builder()
                .serviceHttpUrl(pulsar.getHttpServiceUrl())
                .build();

            assertThat(pulsarAdmin.functions().getFunctions("public", "default")).hasSize(0);
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
