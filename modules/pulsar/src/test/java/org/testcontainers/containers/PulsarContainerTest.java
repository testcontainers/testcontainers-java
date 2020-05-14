package org.testcontainers.containers;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.common.functions.FunctionConfig;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class PulsarContainerTest {

    public static final String TEST_TOPIC = "test_topic";

    public static final String TEST_OUTPUT_TOPIC = "test_output_topic";

    @Test
    public void testUsage() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer()) {
            pulsar.start();
            testPulsarFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    @Test
    public void shouldWaitForFunctionsWorkerStarted() throws PulsarClientException, PulsarAdminException {
        try (PulsarContainer pulsar = new PulsarContainer("2.3.1")
            .withClasspathResourceMapping("functions.jar", "/functions.jar", BindMode.READ_ONLY)
            .withFunctionsWorker()) {

            pulsar.start();

            PulsarAdmin pulsarAdmin = PulsarAdmin.builder()
                .serviceHttpUrl(pulsar.getHttpServiceUrl())
                .build();

            pulsarAdmin.functions().createFunctionWithUrl(
                FunctionConfig.builder()
                    .tenant("public")
                    .namespace("default")
                    .name("exclamation")
                    .inputs(Collections.singletonList(TEST_TOPIC))
                    .output(TEST_OUTPUT_TOPIC)
                    .className("com.example.IdentityFunction")
                    .runtime(FunctionConfig.Runtime.JAVA).build(),
                "file:/functions.jar"
            );
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
